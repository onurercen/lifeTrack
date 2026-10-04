-- When the run happened, separate from when it was recorded.
-- Existing runs are assumed to have happened when they were recorded.
alter table runs add column run_at timestamp(6);
update runs set run_at = created_at;
alter table runs alter column run_at set not null;

-- Most people don't know their calories; make it optional.
alter table runs alter column calories_burned drop not null;

-- Descriptions and links are optional; descriptions get room to breathe.
alter table books alter column description drop not null;
alter table books alter column description set data type varchar(2000);

alter table media alter column description drop not null;
alter table media alter column description set data type varchar(2000);
alter table media alter column url drop not null;
alter table media alter column url set data type varchar(2048);

-- Every list query filters by user and sorts by date.
create index idx_runs_user_run_at on runs (user_id, run_at desc);
create index idx_books_user_created_at on books (user_id, created_at desc);
create index idx_media_user_created_at on media (user_id, created_at desc);
