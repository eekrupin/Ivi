import "@typespec/http";
import "@typespec/openapi";
import "@typespec/rest";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.Rest;
using TypeSpec.OpenAPI;

@service(#{ title: "Ivi API" })
@info(#{
  version: "0.1.0",
  summary: "Backend API and sync contract for the Ivi family pet tracker."
})
@server("https://api.ivi.example", "Primary API endpoint")
namespace IviApi;

@format("uuid")
scalar Uuid extends string;

@minLength(1)
@maxLength(256)
scalar CursorToken extends string;

@minLength(1)
@maxLength(64)
scalar DeviceId extends string;

@minLength(1)
@maxLength(128)
scalar InviteCode extends string;

@minLength(1)
@maxLength(160)
scalar PhotoRevision extends string;

@minLength(1)
@maxLength(256)
scalar ErrorCode extends string;

@minLength(1)
@maxLength(280)
scalar ShortText extends string;

enum MembershipRole {
  OWNER,
  MEMBER,
}

enum MembershipStatus {
  ACTIVE,
  REVOKED,
}

enum InviteStatus {
  PENDING,
  ACCEPTED,
  REVOKED,
  EXPIRED,
}

enum EventCategory {
  TICK,
  DEWORMING,
  VACCINATION,
  CHECKUP,
  OTHER,
}

enum PetEventStatus {
  ACTIVE,
  COMPLETED,
  ARCHIVED,
}

enum SyncEntityType {
  USER,
  PET,
  PET_MEMBERSHIP,
  EVENT_TYPE,
  PET_EVENT,
  WEIGHT_ENTRY,
}

enum ConflictReason {
  VERSION_MISMATCH,
  CURSOR_EXPIRED,
  FORBIDDEN,
  INVALID_REFERENCE,
}

enum PhotoUploadStatus {
  STORED,
  REPLACED,
}

model ErrorDetail {
  field?: string;
  issue: string;
}

model ErrorBody {
  code: ErrorCode;
  message: ShortText;
  details?: ErrorDetail[];
}

model BadRequestError {
  @statusCode statusCode: 400;
  error: ErrorBody;
}

model UnauthorizedError {
  @statusCode statusCode: 401;
  error: ErrorBody;
}

model ForbiddenError {
  @statusCode statusCode: 403;
  error: ErrorBody;
}

model NotFoundError {
  @statusCode statusCode: 404;
  error: ErrorBody;
}

model ConflictError {
  @statusCode statusCode: 409;
  error: ErrorBody;
  conflicts: ConflictDto[];
}

model SyncRecordBase {
  id: Uuid;
  version: int64;
  updatedAt: utcDateTime;
  deletedAt?: utcDateTime | null;
}

model UserProfile extends SyncRecordBase {
  email: string;
  displayName: string;
  createdAt: utcDateTime;
}

model Pet extends SyncRecordBase {
  name: string;
  birthDate?: plainDate | null;
  photoRevision?: PhotoRevision | null;
  createdAt: utcDateTime;
}

model PetMembership extends SyncRecordBase {
  petId: Uuid;
  userId: Uuid;
  role: MembershipRole;
  status: MembershipStatus;
  createdAt: utcDateTime;
}

model EventType extends SyncRecordBase {
  petId: Uuid;
  name: string;
  category: EventCategory;
  defaultDurationDays?: int32 | null;
  isActive: boolean;
  colorArgb?: int32 | null;
  iconKey?: string | null;
  createdAt: utcDateTime;
}

model PetEvent extends SyncRecordBase {
  petId: Uuid;
  eventTypeId: Uuid;
  eventDate: plainDate;
  dueDate?: plainDate | null;
  comment?: string | null;
  notificationsEnabled: boolean;
  status: PetEventStatus;
  createdAt: utcDateTime;
}

model WeightEntry extends SyncRecordBase {
  petId: Uuid;
  date: plainDate;
  weightGrams: int32;
  comment?: string | null;
  createdAt: utcDateTime;
}

model InviteDto {
  id: Uuid;
  petId: Uuid;
  code: InviteCode;
  status: InviteStatus;
  expiresAt: utcDateTime;
  createdAt: utcDateTime;
}

model AuthTokenPairDto {
  tokenType: "Bearer";
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: int32;
}

model HealthResponse {
  @statusCode statusCode: 200;
  status: "ok";
  service: "ivi-backend";
  contractVersion: "0.1.0";
}

model AuthRequestBase {
  email: string;
  password: string;
}

model RegisterRequest extends AuthRequestBase {
  displayName: string;
}

model LoginRequest extends AuthRequestBase {}

model RefreshRequest {
  refreshToken: string;
}

model AuthResponse {
  @statusCode statusCode: 200;
  tokens: AuthTokenPairDto;
  user: UserProfile;
}

model MeResponse {
  @statusCode statusCode: 200;
  user: UserProfile;
  currentPetId?: Uuid | null;
  memberships: PetMembership[];
}

model CreatePetRequest {
  name: string;
  birthDate?: plainDate | null;
}

model CurrentPetResponse {
  @statusCode statusCode: 200;
  pet: Pet;
}

model CreatePetResponse {
  @statusCode statusCode: 201;
  pet: Pet;
}

model CreateInviteRequest {
  expiresInHours?: int32;
}

model CreateInviteResponse {
  @statusCode statusCode: 201;
  invite: InviteDto;
}

model AcceptInviteRequest {
  code: InviteCode;
}

model AcceptInviteResponse {
  @statusCode statusCode: 200;
  invite: InviteDto;
  pet: Pet;
  membership: PetMembership;
}

model SyncSnapshot {
  users: UserProfile[];
  pets: Pet[];
  memberships: PetMembership[];
  eventTypes: EventType[];
  petEvents: PetEvent[];
  weightEntries: WeightEntry[];
}

model SyncBootstrapResponse {
  @statusCode statusCode: 200;
  cursor: CursorToken;
  snapshot: SyncSnapshot;
  notes?: string[];
}

model Tombstone {
  entityType: SyncEntityType;
  id: Uuid;
  deletedAt: utcDateTime;
  version: int64;
}

model SyncEntityChanges {
  users: UserProfile[];
  pets: Pet[];
  memberships: PetMembership[];
  eventTypes: EventType[];
  petEvents: PetEvent[];
  weightEntries: WeightEntry[];
}

model SyncChangesResponse {
  @statusCode statusCode: 200;
  cursor: CursorToken;
  hasMore: boolean;
  changes: SyncEntityChanges;
  tombstones: Tombstone[];
}

model PetWriteModel {
  name: string;
  birthDate?: plainDate | null;
  photoRevision?: PhotoRevision | null;
}

model EventTypeWriteModel {
  petId: Uuid;
  name: string;
  category: EventCategory;
  defaultDurationDays?: int32 | null;
  isActive: boolean;
  colorArgb?: int32 | null;
  iconKey?: string | null;
}

model PetEventWriteModel {
  petId: Uuid;
  eventTypeId: Uuid;
  eventDate: plainDate;
  dueDate?: plainDate | null;
  comment?: string | null;
  notificationsEnabled: boolean;
  status: PetEventStatus;
}

model WeightEntryWriteModel {
  petId: Uuid;
  date: plainDate;
  weightGrams: int32;
  comment?: string | null;
}

model SyncMutationMeta {
  clientMutationId?: Uuid;
  entityId: Uuid;
  baseVersion?: int64;
}

model UpsertPetMutation extends SyncMutationMeta {
  entityType: "PET";
  operation: "UPSERT";
  payload: PetWriteModel;
}

model DeletePetMutation extends SyncMutationMeta {
  entityType: "PET";
  operation: "DELETE";
}

model UpsertEventTypeMutation extends SyncMutationMeta {
  entityType: "EVENT_TYPE";
  operation: "UPSERT";
  payload: EventTypeWriteModel;
}

model DeleteEventTypeMutation extends SyncMutationMeta {
  entityType: "EVENT_TYPE";
  operation: "DELETE";
}

model UpsertPetEventMutation extends SyncMutationMeta {
  entityType: "PET_EVENT";
  operation: "UPSERT";
  payload: PetEventWriteModel;
}

model DeletePetEventMutation extends SyncMutationMeta {
  entityType: "PET_EVENT";
  operation: "DELETE";
}

model UpsertWeightEntryMutation extends SyncMutationMeta {
  entityType: "WEIGHT_ENTRY";
  operation: "UPSERT";
  payload: WeightEntryWriteModel;
}

model DeleteWeightEntryMutation extends SyncMutationMeta {
  entityType: "WEIGHT_ENTRY";
  operation: "DELETE";
}

union SyncMutation {
  UpsertPetMutation,
  DeletePetMutation,
  UpsertEventTypeMutation,
  DeleteEventTypeMutation,
  UpsertPetEventMutation,
  DeletePetEventMutation,
  UpsertWeightEntryMutation,
  DeleteWeightEntryMutation,
}

union SyncRecord {
  UserProfile,
  Pet,
  PetMembership,
  EventType,
  PetEvent,
  WeightEntry,
}

model SyncPushRequest {
  deviceId: DeviceId;
  lastKnownCursor?: CursorToken | null;
  mutations: SyncMutation[];
}

model AcceptedMutation {
  clientMutationId?: Uuid;
  entityType: SyncEntityType;
  entityId: Uuid;
  version: int64;
}

model ConflictDto {
  entityType: SyncEntityType;
  entityId: Uuid;
  clientMutationId?: Uuid;
  baseVersion?: int64;
  serverVersion: int64;
  reason: ConflictReason;
  serverRecord?: SyncRecord | null;
}

model SyncPushResponse {
  @statusCode statusCode: 200;
  accepted: AcceptedMutation[];
  conflicts: ConflictDto[];
  cursor: CursorToken;
  requiresBootstrap: boolean;
}

model PhotoUploadResponse {
  @statusCode statusCode: 200;
  petId: Uuid;
  photoRevision: PhotoRevision;
  status: PhotoUploadStatus;
}

model PhotoDeleteResponse {
  @statusCode statusCode: 200;
  petId: Uuid;
  photoRevision?: PhotoRevision | null;
  deleted: boolean;
}

@tag("Health")
@route("/health")
namespace HealthApi {
  @get
  op getHealth(): HealthResponse;
}

@tag("Auth")
@route("/v1/auth")
namespace AuthApi {
  @post
  @route("/register")
  op register(@body body: RegisterRequest): AuthResponse | BadRequestError | ConflictError;

  @post
  @route("/login")
  op login(@body body: LoginRequest): AuthResponse | BadRequestError | UnauthorizedError;

  @post
  @route("/refresh")
  op refresh(@body body: RefreshRequest): AuthResponse | BadRequestError | UnauthorizedError;
}

@tag("Me")
@route("/v1/me")
namespace MeApi {
  @get
  op getMe(): MeResponse | UnauthorizedError;
}

@tag("Pets")
@route("/v1/pets")
namespace PetsApi {
  @post
  op createPet(@body body: CreatePetRequest): CreatePetResponse | BadRequestError | ConflictError;

  @get
  @route("/current")
  op getCurrentPet(): CurrentPetResponse | UnauthorizedError | NotFoundError;
}

@tag("Invites")
@route("/v1")
namespace InvitesApi {
  @post
  @route("/pets/{petId}/invites")
  op createInvite(@path petId: Uuid, @body body: CreateInviteRequest): CreateInviteResponse | UnauthorizedError | ForbiddenError | NotFoundError;

  @post
  @route("/invites/accept")
  op acceptInvite(@body body: AcceptInviteRequest): AcceptInviteResponse | BadRequestError | UnauthorizedError | ConflictError;
}

@tag("Sync")
@route("/v1/sync")
namespace SyncApi {
  @get
  @route("/bootstrap")
  op bootstrap(): SyncBootstrapResponse | UnauthorizedError | NotFoundError;

  @get
  @route("/changes")
  op getChanges(@query cursor: CursorToken): SyncChangesResponse | UnauthorizedError | ConflictError;

  @post
  @route("/push")
  op push(@body body: SyncPushRequest): SyncPushResponse | BadRequestError | UnauthorizedError | ConflictError;
}

@tag("Photo")
@route("/v1/pets")
namespace PetPhotoApi {
  @put
  @route("/{petId}/photo")
  op uploadPhoto(@path petId: Uuid, @header contentType: string, @body body: bytes): PhotoUploadResponse | BadRequestError | UnauthorizedError | ForbiddenError | NotFoundError;

  @delete
  @route("/{petId}/photo")
  op deletePhoto(@path petId: Uuid): PhotoDeleteResponse | UnauthorizedError | ForbiddenError | NotFoundError;
}
