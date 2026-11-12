create table reactions (
    fingerprint varchar(64) primary key,
    created_at  timestamp not null,
    payload     text not null
);
