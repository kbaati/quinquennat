create table simulations (
    id         varchar(16) primary key,
    created_at timestamp   not null,
    payload    text        not null
);
