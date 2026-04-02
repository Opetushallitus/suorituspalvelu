CREATE TABLE IF NOT EXISTS koski_opiskeluoikeus_skip (
    henkilo_oid varchar(255),
    opiskeluoikeus_oid varchar(255),
    selite varchar(255),
    aikaleima timestamptz,
    primary key (henkilo_oid, opiskeluoikeus_oid)
);
