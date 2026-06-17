CREATE TABLE theme
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    thumbnail   VARCHAR(255) NOT NULL,
    price       BIGINT       NOT NULL,
    deleted_at  TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE reservation_time
(
    id           BIGINT NOT NULL AUTO_INCREMENT,
    start_at     TIME   NOT NULL,
    deleted_at   TIMESTAMP,
    delete_token BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (start_at, delete_token)
);

CREATE TABLE reservation
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    guest_name       VARCHAR(255) NOT NULL,
    date             DATE         NOT NULL,
    time_id          BIGINT       NOT NULL,
    theme_id         BIGINT       NOT NULL,
    cancel_token     BIGINT       NOT NULL DEFAULT 0,
    status           VARCHAR(50)  NOT NULL,
    last_modified_at TIMESTAMP    NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (guest_name, date, time_id, theme_id, cancel_token),
    FOREIGN KEY (time_id) REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id)
);

CREATE TABLE payment_session
(
    order_id       VARCHAR(255) NOT NULL,
    reservation_id BIGINT       NOT NULL,
    amount         BIGINT       NOT NULL,

    PRIMARY KEY (order_id),
    FOREIGN KEY (reservation_id) REFERENCES reservation (id)
);

CREATE TABLE toss_payment
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT       NOT NULL,
    payment_key    VARCHAR(255) NOT NULL,
    amount         BIGINT       NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    approved_at    TIMESTAMP    NOT NULL,
    requested_at   TIMESTAMP    NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (payment_key),
    FOREIGN KEY (reservation_id) REFERENCES reservation (id)
);
