import fs from 'node:fs/promises'

const baseUrl = process.env.IVI_BASE_URL ?? 'http://127.0.0.1:8080'

function fail(message) {
  throw new Error(message)
}

async function api(path, { method = 'GET', token, body } = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })

  const text = await response.text()
  let json = null
  if (text) {
    try {
      json = JSON.parse(text)
    } catch {
      json = { raw: text }
    }
  }

  return {
    status: response.status,
    ok: response.ok,
    json,
    text,
  }
}

function expectStatus(response, expected, context) {
  if (response.status !== expected) {
    fail(`${context}: expected ${expected}, got ${response.status}, body=${JSON.stringify(response.json)}`)
  }
}

function randomEmail(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`
}

function createUuid() {
  return crypto.randomUUID()
}

const report = {
  checkedAt: new Date().toISOString(),
  baseUrl,
  steps: [],
  facts: {},
  issues: [],
}

function step(name, details) {
  report.steps.push({ name, ...details })
}

function issue(severity, title, details) {
  report.issues.push({ severity, title, details })
}

async function main() {
  const health = await api('/health')
  expectStatus(health, 200, 'health')
  step('health', { status: 'passed', response: health.json })

  const user1 = {
    email: randomEmail('user1'),
    password: 'password123',
    displayName: 'Пользователь Один',
  }
  const user2 = {
    email: randomEmail('user2'),
    password: 'password123',
    displayName: 'Пользователь Два',
  }

  const register1 = await api('/v1/auth/register', {
    method: 'POST',
    body: user1,
  })
  expectStatus(register1, 200, 'register user1')
  const user1Tokens = register1.json.tokens
  step('register_user1', { status: 'passed', userId: register1.json.user.id })

  const login1Bad = await api('/v1/auth/login', {
    method: 'POST',
    body: { email: user1.email, password: 'wrongpass' },
  })
  if (login1Bad.status !== 401) {
    issue('important', 'Неверный логин не вернул 401', login1Bad.json)
  }
  step('login_user1_invalid_password', { status: login1Bad.status === 401 ? 'passed' : 'failed', statusCode: login1Bad.status })

  const login1 = await api('/v1/auth/login', {
    method: 'POST',
    body: { email: user1.email, password: user1.password },
  })
  expectStatus(login1, 200, 'login user1')
  const login1Tokens = login1.json.tokens
  step('login_user1', { status: 'passed' })

  const meBeforePet = await api('/v1/me', { token: login1Tokens.accessToken })
  expectStatus(meBeforePet, 200, 'me before pet')
  step('me_before_pet', { status: 'passed', currentPetId: meBeforePet.json.currentPetId })

  const createPet = await api('/v1/pets', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: { name: 'Иви E2E', birthDate: '2023-04-10' },
  })
  expectStatus(createPet, 201, 'create pet')
  const petId = createPet.json.pet.id
  step('create_pet', { status: 'passed', petId })

  const createPetAgain = await api('/v1/pets', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: { name: 'Иви E2E second', birthDate: '2023-05-10' },
  })
  expectStatus(createPetAgain, 409, 'create second pet for same user')
  if (createPetAgain.json?.error?.code !== 'pet_already_exists_for_user') {
    fail(`expected pet_already_exists_for_user for second pet, got ${JSON.stringify(createPetAgain.json)}`)
  }
  step('create_second_pet_same_user', { status: 'passed', statusCode: createPetAgain.status })

  const currentPet = await api('/v1/pets/current', { token: login1Tokens.accessToken })
  expectStatus(currentPet, 200, 'current pet')
  if (currentPet.json.membership.role !== 'OWNER') {
    fail(`current pet membership role mismatch: ${JSON.stringify(currentPet.json)}`)
  }
  step('get_current_pet', { status: 'passed', petId: currentPet.json.pet.id })

  const createInvite = await api(`/v1/pets/${petId}/invites`, {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: { expiresInHours: 24 },
  })
  expectStatus(createInvite, 201, 'create invite')
  const inviteCode = createInvite.json.invite.code
  step('create_invite', { status: 'passed', inviteId: createInvite.json.invite.id })

  const register2 = await api('/v1/auth/register', {
    method: 'POST',
    body: user2,
  })
  expectStatus(register2, 200, 'register user2')
  const user2Tokens = register2.json.tokens
  step('register_user2', { status: 'passed', userId: register2.json.user.id })

  const acceptInvite = await api('/v1/invites/accept', {
    method: 'POST',
    token: user2Tokens.accessToken,
    body: { code: inviteCode },
  })
  expectStatus(acceptInvite, 200, 'accept invite')
  if (acceptInvite.json.membership.role !== 'MEMBER') {
    fail(`accept invite membership role mismatch: ${JSON.stringify(acceptInvite.json)}`)
  }
  step('accept_invite', { status: 'passed', membershipId: acceptInvite.json.membership.id })

  const acceptInviteAgain = await api('/v1/invites/accept', {
    method: 'POST',
    token: user2Tokens.accessToken,
    body: { code: inviteCode },
  })
  expectStatus(acceptInviteAgain, 200, 'accept invite again by same user')
  if (acceptInviteAgain.json.membership.role !== 'MEMBER') {
    fail(`repeat accept invite membership role mismatch: ${JSON.stringify(acceptInviteAgain.json)}`)
  }
  step('accept_invite_again_same_user', { status: 'passed', membershipId: acceptInviteAgain.json.membership.id })

  const user3 = {
    email: randomEmail('user3'),
    password: 'password123',
    displayName: 'Пользователь Три',
  }
  const user4 = {
    email: randomEmail('user4'),
    password: 'password123',
    displayName: 'Пользователь Четыре',
  }
  const register3 = await api('/v1/auth/register', {
    method: 'POST',
    body: user3,
  })
  expectStatus(register3, 200, 'register user3')
  const user3Tokens = register3.json.tokens
  step('register_user3', { status: 'passed', userId: register3.json.user.id })

  const acceptInviteByOtherUser = await api('/v1/invites/accept', {
    method: 'POST',
    token: user3Tokens.accessToken,
    body: { code: inviteCode },
  })
  expectStatus(acceptInviteByOtherUser, 409, 'accept invite by other user after accepted')
  if (acceptInviteByOtherUser.json?.error?.code !== 'invite_not_active') {
    fail(`expected invite_not_active for other user, got ${JSON.stringify(acceptInviteByOtherUser.json)}`)
  }
  step('accept_invite_other_user_after_accepted', { status: 'passed', statusCode: acceptInviteByOtherUser.status })

  const createPetUser3 = await api('/v1/pets', {
    method: 'POST',
    token: user3Tokens.accessToken,
    body: { name: 'Иви E2E user3', birthDate: '2024-01-15' },
  })
  expectStatus(createPetUser3, 201, 'create pet user3')
  const petIdUser3 = createPetUser3.json.pet.id
  step('create_pet_user3', { status: 'passed', petId: petIdUser3 })

  const createInviteUser3 = await api(`/v1/pets/${petIdUser3}/invites`, {
    method: 'POST',
    token: user3Tokens.accessToken,
    body: { expiresInHours: 24 },
  })
  expectStatus(createInviteUser3, 201, 'create invite user3')
  const inviteCodeUser3 = createInviteUser3.json.invite.code
  step('create_invite_user3', { status: 'passed', inviteId: createInviteUser3.json.invite.id })

  const register4 = await api('/v1/auth/register', {
    method: 'POST',
    body: user4,
  })
  expectStatus(register4, 200, 'register user4')
  const user4Tokens = register4.json.tokens
  step('register_user4', { status: 'passed', userId: register4.json.user.id })

  const createPetUser4 = await api('/v1/pets', {
    method: 'POST',
    token: user4Tokens.accessToken,
    body: { name: 'Иви E2E user4', birthDate: '2024-02-20' },
  })
  expectStatus(createPetUser4, 201, 'create pet user4')
  step('create_pet_user4', { status: 'passed', petId: createPetUser4.json.pet.id })

  const acceptInviteByAlreadyBoundUser = await api('/v1/invites/accept', {
    method: 'POST',
    token: user4Tokens.accessToken,
    body: { code: inviteCodeUser3 },
  })
  expectStatus(acceptInviteByAlreadyBoundUser, 409, 'accept invite by already bound user')
  if (acceptInviteByAlreadyBoundUser.json?.error?.code !== 'user_already_bound_to_pet') {
    fail(`expected user_already_bound_to_pet for already bound user, got ${JSON.stringify(acceptInviteByAlreadyBoundUser.json)}`)
  }
  step('accept_invite_already_bound_user', { status: 'passed', statusCode: acceptInviteByAlreadyBoundUser.status })

  const user2CurrentPet = await api('/v1/pets/current', { token: user2Tokens.accessToken })
  expectStatus(user2CurrentPet, 200, 'user2 current pet')
  if (user2CurrentPet.json.membership.role !== 'MEMBER') {
    fail(`user2 current pet membership role mismatch: ${JSON.stringify(user2CurrentPet.json)}`)
  }
  step('user2_current_pet', { status: 'passed', petId: user2CurrentPet.json.pet.id })

  const bootstrap1 = await api('/v1/sync/bootstrap', { token: login1Tokens.accessToken })
  expectStatus(bootstrap1, 200, 'bootstrap user1')
  const bootstrapCursor = bootstrap1.json.cursor
  step('bootstrap_user1', {
    status: 'passed',
    cursor: bootstrapCursor,
    snapshotCounts: {
      pets: bootstrap1.json.snapshot.pets.length,
      memberships: bootstrap1.json.snapshot.memberships.length,
      eventTypes: bootstrap1.json.snapshot.eventTypes.length,
      petEvents: bootstrap1.json.snapshot.petEvents.length,
      weightEntries: bootstrap1.json.snapshot.weightEntries.length,
    },
  })

  const eventTypeId = createUuid()
  const pushEventType = await api('/v1/sync/push', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: {
      deviceId: 'e2e-user1-device',
      lastKnownCursor: bootstrapCursor,
      mutations: [
        {
          clientMutationId: 'mut-event-type-1',
          entityId: eventTypeId,
          baseVersion: null,
          entityType: 'EVENT_TYPE',
          operation: 'UPSERT',
          payload: {
            petId,
            name: 'E2E Бравекто',
            category: 'TICK_PROTECTION',
            defaultDurationDays: 30,
            isActive: true,
            colorArgb: 16755200,
            iconKey: 'pill',
          },
        },
      ],
    },
  })
  expectStatus(pushEventType, 200, 'push event type')
  if (pushEventType.json.accepted.length !== 1) {
    fail(`push event type accepted mismatch: ${JSON.stringify(pushEventType.json)}`)
  }
  const eventTypeVersion1 = pushEventType.json.accepted[0].version
  step('push_event_type', { status: 'passed', version: eventTypeVersion1, cursor: pushEventType.json.cursor })

  const changesForUser2EventType = await api(`/v1/sync/changes?cursor=${encodeURIComponent(bootstrapCursor)}`, {
    token: user2Tokens.accessToken,
  })
  expectStatus(changesForUser2EventType, 200, 'changes user2 after event type')
  step('changes_user2_after_event_type', {
    status: 'passed',
    eventTypes: changesForUser2EventType.json.changes.eventTypes.length,
    petEvents: changesForUser2EventType.json.changes.petEvents.length,
    weightEntries: changesForUser2EventType.json.changes.weightEntries.length,
  })

  const petEventId = createUuid()
  const pushPetEvent = await api('/v1/sync/push', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: {
      deviceId: 'e2e-user1-device',
      lastKnownCursor: pushEventType.json.cursor,
      mutations: [
        {
          clientMutationId: 'mut-pet-event-1',
          entityId: petEventId,
          baseVersion: null,
          entityType: 'PET_EVENT',
          operation: 'UPSERT',
          payload: {
            petId,
            eventTypeId,
            eventDate: '2026-04-16',
            dueDate: '2026-05-16',
            comment: 'Первое событие E2E',
            notificationsEnabled: true,
            status: 'ACTIVE',
          },
        },
      ],
    },
  })
  expectStatus(pushPetEvent, 200, 'push pet event')
  const petEventVersion1 = pushPetEvent.json.accepted[0].version
  step('push_pet_event', { status: 'passed', version: petEventVersion1, cursor: pushPetEvent.json.cursor })

  const weightEntryId = createUuid()
  const pushWeight = await api('/v1/sync/push', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: {
      deviceId: 'e2e-user1-device',
      lastKnownCursor: pushPetEvent.json.cursor,
      mutations: [
        {
          clientMutationId: 'mut-weight-1',
          entityId: weightEntryId,
          baseVersion: null,
          entityType: 'WEIGHT_ENTRY',
          operation: 'UPSERT',
          payload: {
            petId,
            date: '2026-04-16',
            weightGrams: 12345,
            comment: 'Вес E2E',
          },
        },
      ],
    },
  })
  expectStatus(pushWeight, 200, 'push weight')
  const weightVersion1 = pushWeight.json.accepted[0].version
  step('push_weight', { status: 'passed', version: weightVersion1, cursor: pushWeight.json.cursor })

  const bootstrap2 = await api('/v1/sync/bootstrap', { token: user2Tokens.accessToken })
  expectStatus(bootstrap2, 200, 'bootstrap user2')
  step('bootstrap_user2', {
    status: 'passed',
    snapshotCounts: {
      eventTypes: bootstrap2.json.snapshot.eventTypes.length,
      petEvents: bootstrap2.json.snapshot.petEvents.length,
      weightEntries: bootstrap2.json.snapshot.weightEntries.length,
    },
  })

  const user2UpdateEventType = await api('/v1/sync/push', {
    method: 'POST',
    token: user2Tokens.accessToken,
    body: {
      deviceId: 'e2e-user2-device',
      lastKnownCursor: bootstrap2.json.cursor,
      mutations: [
        {
          clientMutationId: 'mut-event-type-2',
          entityId: eventTypeId,
          baseVersion: eventTypeVersion1,
          entityType: 'EVENT_TYPE',
          operation: 'UPSERT',
          payload: {
            petId,
            name: 'E2E Бравекто обновлено user2',
            category: 'TICK_PROTECTION',
            defaultDurationDays: 45,
            isActive: true,
            colorArgb: 16755200,
            iconKey: 'pill',
          },
        },
      ],
    },
  })
  expectStatus(user2UpdateEventType, 200, 'user2 update event type')
  const eventTypeVersion2 = user2UpdateEventType.json.accepted[0].version
  step('user2_updates_event_type', { status: 'passed', version: eventTypeVersion2 })

  const conflictResponse = await api('/v1/sync/push', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: {
      deviceId: 'e2e-user1-device',
      lastKnownCursor: pushWeight.json.cursor,
      mutations: [
        {
          clientMutationId: 'mut-event-type-conflict',
          entityId: eventTypeId,
          baseVersion: eventTypeVersion1,
          entityType: 'EVENT_TYPE',
          operation: 'UPSERT',
          payload: {
            petId,
            name: 'E2E user1 stale update',
            category: 'TICK_PROTECTION',
            defaultDurationDays: 60,
            isActive: true,
            colorArgb: 16755200,
            iconKey: 'pill',
          },
        },
      ],
    },
  })
  expectStatus(conflictResponse, 200, 'conflict response')
  if (conflictResponse.json.conflicts.length !== 1 || conflictResponse.json.conflicts[0].reason !== 'VERSION_MISMATCH') {
    fail(`expected one VERSION_MISMATCH conflict, got ${JSON.stringify(conflictResponse.json)}`)
  }
  step('version_conflict', {
    status: 'passed',
    serverVersion: conflictResponse.json.conflicts[0].serverVersion,
    hasServerRecord: Boolean(conflictResponse.json.conflicts[0].serverRecord),
  })

  const replayAfterConflict = await api('/v1/sync/push', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: {
      deviceId: 'e2e-user1-device',
      lastKnownCursor: conflictResponse.json.cursor,
      mutations: [
        {
          clientMutationId: 'mut-event-type-retry',
          entityId: eventTypeId,
          baseVersion: eventTypeVersion2,
          entityType: 'EVENT_TYPE',
          operation: 'UPSERT',
          payload: {
            petId,
            name: 'E2E user1 retry after conflict',
            category: 'TICK_PROTECTION',
            defaultDurationDays: 60,
            isActive: true,
            colorArgb: 16755200,
            iconKey: 'pill',
          },
        },
      ],
    },
  })
  expectStatus(replayAfterConflict, 200, 'retry after conflict')
  if (replayAfterConflict.json.accepted.length !== 1) {
    fail(`retry after conflict was not accepted: ${JSON.stringify(replayAfterConflict.json)}`)
  }
  step('retry_after_conflict', { status: 'passed', version: replayAfterConflict.json.accepted[0].version })

  const invalidReferenceResponse = await api('/v1/sync/push', {
    method: 'POST',
    token: login1Tokens.accessToken,
    body: {
      deviceId: 'e2e-user1-device',
      lastKnownCursor: replayAfterConflict.json.cursor,
      mutations: [
        {
          clientMutationId: 'mut-invalid-reference',
          entityId: createUuid(),
          baseVersion: null,
          entityType: 'PET_EVENT',
          operation: 'UPSERT',
          payload: {
            petId,
            eventTypeId: createUuid(),
            eventDate: '2026-04-16',
            dueDate: '2026-05-16',
            comment: 'Broken ref',
            notificationsEnabled: true,
            status: 'ACTIVE',
          },
        },
      ],
    },
  })
  expectStatus(invalidReferenceResponse, 200, 'invalid reference conflict')
  if (invalidReferenceResponse.json.conflicts[0]?.reason !== 'INVALID_REFERENCE') {
    issue('important', 'INVALID_REFERENCE не воспроизвёлся как ожидалось', invalidReferenceResponse.json)
  }
  step('invalid_reference_conflict', {
    status: invalidReferenceResponse.json.conflicts[0]?.reason === 'INVALID_REFERENCE' ? 'passed' : 'failed',
    serverVersion: invalidReferenceResponse.json.conflicts[0]?.serverVersion ?? null,
  })

  const refresh1 = await api('/v1/auth/refresh', {
    method: 'POST',
    body: { refreshToken: login1Tokens.refreshToken },
  })
  expectStatus(refresh1, 200, 'refresh user1')
  step('refresh_user1', { status: 'passed' })

  const oldRefreshReuse = await api('/v1/auth/refresh', {
    method: 'POST',
    body: { refreshToken: login1Tokens.refreshToken },
  })
  if (oldRefreshReuse.status !== 401) {
    issue('important', 'Старый refresh token повторно не был отклонён', oldRefreshReuse.json)
  }
  step('reuse_old_refresh_token', { status: oldRefreshReuse.status === 401 ? 'passed' : 'failed', statusCode: oldRefreshReuse.status })

  report.facts = {
    petId,
    inviteCode,
    eventTypeId,
    petEventId,
    weightEntryId,
    eventTypeVersion1,
    eventTypeVersion2,
    weightVersion1,
    petEventVersion1,
  }

  await fs.writeFile('/tmp/ivi-e2e-report.json', JSON.stringify(report, null, 2))
  console.log(JSON.stringify(report, null, 2))
}

main().catch(async (error) => {
  const failedReport = {
    ...report,
    failed: true,
    error: {
      message: error.message,
      stack: error.stack,
    },
  }
  await fs.writeFile('/tmp/ivi-e2e-report.json', JSON.stringify(failedReport, null, 2))
  console.error(error)
  process.exit(1)
})
