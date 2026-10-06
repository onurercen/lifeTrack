-- Reading/watching state, progress and ratings.
-- Existing books are assumed to be on the reading list; existing media entries were
-- added as things already watched ("İzlediğin film, dizi...").
alter table books add column status varchar(20) default 'WANT_TO_READ' not null;
alter table books add column page_count integer;
alter table books add column current_page integer;
alter table books add column rating integer;
alter table books add column started_on date;
alter table books add column finished_on date;

alter table books add constraint ck_books_status check (status in ('WANT_TO_READ', 'READING', 'FINISHED'));
alter table books add constraint ck_books_rating check (rating between 1 and 5);
alter table books add constraint ck_books_pages check (page_count > 0 and current_page >= 0);

alter table media add column status varchar(20) default 'COMPLETED' not null;
alter table media add column rating integer;
alter table media add column finished_on date;

alter table media add constraint ck_media_status check (status in ('PLANNED', 'IN_PROGRESS', 'COMPLETED'));
alter table media add constraint ck_media_rating check (rating between 1 and 5);

-- The list screens filter by status.
create index idx_books_user_status on books (user_id, status);
create index idx_media_user_status on media (user_id, status);
