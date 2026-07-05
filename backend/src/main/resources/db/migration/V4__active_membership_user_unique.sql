DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pet_memberships
        WHERE status = 'ACTIVE'
        GROUP BY user_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create uq_pet_memberships_user_id_active: some users already have more than one ACTIVE pet membership. Resolve duplicate ACTIVE memberships manually before running this migration.';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_pet_memberships_user_id_active
    ON pet_memberships(user_id)
    WHERE status = 'ACTIVE';
