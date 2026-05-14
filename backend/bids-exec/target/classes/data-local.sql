merge into bids_datasource (id, code, name, jdbc_url, username, password, driver_class_name, max_pool_size, active) key(id) values
('local-ds', 'local', '本地测试数据源', 'jdbc:h2:file:/tmp/bids-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE', 'sa', '', 'org.h2.Driver', 5, true);

merge into bids_sql_model (id, code, name, datasource_code, sql_template, max_rows, status) key(id) values
('order-query', 'order_query', '订单查询', 'local',
 'select order_id, user_name, amount, create_time from t_order where 1 = 1 <#if userName?? && userName?has_content> and user_name like concat(''%'', :userName, ''%'') </#if> order by create_time desc',
 100, 'PUBLISHED');

merge into bids_form_field (id, model_id, field_name, label, field_type, required, default_value, options_json, sort_order) key(id) values
('order-user-name', 'order-query', 'userName', '用户名', 'TEXT', false, null, null, 1);

merge into bids_result_column (id, model_id, column_name, label, visible, mask_type, sort_order) key(id) values
('order-col-id', 'order-query', 'order_id', '订单号', true, null, 1),
('order-col-user', 'order-query', 'user_name', '用户名', true, null, 2),
('order-col-amount', 'order-query', 'amount', '金额', true, null, 3),
('order-col-time', 'order-query', 'create_time', '创建时间', true, null, 4);

create table if not exists t_order (
  order_id varchar(32) primary key,
  user_name varchar(64) not null,
  amount decimal(12,2) not null,
  create_time timestamp not null
);

merge into t_order key(order_id) values
('O1001', 'alice', 128.50, timestamp '2026-05-14 10:00:00'),
('O1002', 'bob', 256.00, timestamp '2026-05-14 11:00:00'),
('O1003', 'alice', 512.20, timestamp '2026-05-14 12:00:00');
