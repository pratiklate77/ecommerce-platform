-- One isolated database per microservice that owns a persistence layer.
-- Runs automatically on first container startup against the default user.
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;