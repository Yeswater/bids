SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

create table if not exists bids_datasource (
  id varchar(36) primary key,
  code varchar(64) not null unique,
  name varchar(128) not null,
  jdbc_url varchar(512) not null,
  username varchar(128) not null,
  password varchar(512) not null,
  driver_class_name varchar(128) not null,
  sql_dialect varchar(32) not null default 'MYSQL',
  max_pool_size int not null,
  active boolean not null default true,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
);

create table if not exists bids_sql_model (
  id varchar(36) primary key,
  code varchar(64) not null unique,
  name varchar(128) not null,
  datasource_code varchar(64) not null,
  sql_template text not null,
  max_rows int not null,
  status varchar(32) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
);

create table if not exists bids_form_field (
  id varchar(36) primary key,
  model_id varchar(36) not null,
  field_name varchar(64) not null,
  label varchar(128) not null,
  field_type varchar(32) not null,
  required boolean not null default false,
  default_value varchar(512),
  options_json text,
  sort_order int not null default 0
);

create table if not exists bids_result_column (
  id varchar(36) primary key,
  model_id varchar(36) not null,
  column_name varchar(128) not null,
  label varchar(128) not null,
  value_type varchar(32) not null default 'TEXT',
  visible boolean not null default true,
  mask_type varchar(32),
  sort_order int not null default 0
);

create table if not exists bids_model_permission (
  id varchar(36) primary key,
  model_id varchar(36) not null,
  username varchar(128),
  role_code varchar(128)
);

create table if not exists bids_execute_log (
  id varchar(36) primary key,
  execute_id varchar(64) not null unique,
  model_code varchar(64) not null,
  username varchar(128) not null,
  final_sql text not null,
  parameters_json text not null,
  success boolean not null,
  error_message text,
  duration_ms bigint not null,
  row_count int not null,
  created_at timestamp not null default current_timestamp
);

create table if not exists t_order (
  order_id varchar(32) primary key,
  user_name varchar(64) not null,
  amount decimal(12,2) not null,
  create_time timestamp not null
);

SET FOREIGN_KEY_CHECKS = 1;
