create table customers
(
    id   uuid primary key,
    name text not null unique
);

create table accounts
(
    id          uuid primary key,
    name        text    not null,
    customer_id uuid    not null references customers (id) on delete cascade,
    balance     integer not null
);

create table transactions
(
    id              uuid primary key,
    from_account_id uuid    not null references accounts (id) on delete cascade,
    to_account_id   uuid references accounts (id),
    amount          integer not null,
    type            text    not null
);


