-- The API now lower-cases e-mails on register/login; bring stored ones in line.
-- Accounts that differ only by case (e.g. "Ayse@x.com" and "ayse@x.com") can't be
-- merged automatically, so they are left untouched instead of failing the unique key.
update users u
set email = lower(u.email)
where u.email <> lower(u.email)
  and not exists (
      select 1 from users other
      where other.id <> u.id and lower(other.email) = lower(u.email)
  );
