# Veritabanı

PostgreSQL 16. Şema **Flyway** ile yönetilir; Hibernate yalnızca doğrular (`ddl-auto: validate`).
Entity ile migration uyuşmazsa uygulama açılmaz.

Migration dosyaları: [`backend/src/main/resources/db/migration`](../backend/src/main/resources/db/migration)

| Versiyon | Dosya | İçerik |
|----------|-------|--------|
| V1 | `V1__initial_schema.sql` | `users`, `runs`, `books`, `media` tabloları (Flyway öncesi Hibernate şemasıyla birebir) |
| V2 | `V2__run_date_and_optional_fields.sql` | `runs.run_at`; kalori, kitap/medya açıklaması ve medya bağlantısı opsiyonel; açıklamalar 2000 karakter; liste indeksleri |
| V3 | `V3__lowercase_emails.sql` | Kayıtlı e-postaları küçük harfe çevirir (API artık e-postayı küçük harfle kaydeder ve arar). Yalnızca büyük/küçük harf farkıyla çakışan hesaplara dokunmaz. |

## Tablolar

**users**: `id`, `name`, `email` (unique, küçük harf), `password` (BCrypt), `created_at`

**runs**: `id`, `user_id`, `distance_km`, `duration_minutes`, `calories_burned` (null olabilir),
`notes` (≤500), `run_at` (koşunun yapıldığı an), `created_at` (kaydın oluşturulduğu an)

**books**: `id`, `user_id`, `title`, `author`, `description` (null olabilir, ≤2000), `created_at`

**media**: `id`, `user_id`, `title`, `type`, `url` (null olabilir, ≤2048), `description` (null olabilir, ≤2000), `created_at`

## Flyway öncesi oluşturulmuş veritabanları

`ddl-auto=update` ile oluşmuş bir veritabanında `flyway_schema_history` tablosu yoktur.
`spring.flyway.baseline-on-migrate=true` ve `baseline-version=1` sayesinde böyle bir veritabanı
ilk açılışta V1 olarak işaretlenir ve yalnızca V2 ve sonrası çalışır. Mevcut koşuların `run_at`
değeri `created_at` ile doldurulur. Veri kaybı olmaz.

## Yeni migration ekleme

1. `db/migration/` altına bir sonraki numarayla dosya ekleyin: `V3__kisa_aciklama.sql`.
2. Entity'yi aynı değişikliğe göre güncelleyin.
3. **Uygulanmış bir migration dosyasını asla değiştirmeyin**; Flyway checksum hatası verir. Düzeltme için yeni versiyon ekleyin.
4. `mvn test` çalıştırın:
   - `PostgresSchemaValidationTest` uygulamayı gerçek (gömülü) PostgreSQL üzerinde açar; entity ile şema uyuşmazsa başarısız olur.
   - `DatabaseMigrationTest` hem boş veritabanını hem Flyway öncesi veritabanının yükseltilmesini dener.
   - Diğer testler H2 (PostgreSQL modu) üzerinde aynı migration'larla çalışır; SQL'in iki veritabanında da çalışması gerekir.
