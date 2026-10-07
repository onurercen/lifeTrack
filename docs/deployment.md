# Yayına alma

Uygulama tek bir adresten sunulur:

| Adres | Ne |
|-------|----|
| `https://<alan-adı>/` | Flutter web uygulaması (telefonda "Ana ekrana ekle" ile uygulama gibi açılır) |
| `https://<alan-adı>/api/...` | Backend API |

Web uygulaması ve API aynı origin'de olduğu için CORS gerekmez. Mobil projede şu an yalnızca **web** platformu var;
Android/iOS uygulaması için önce `flutter create --platforms=android,ios .` ile platform eklenmesi gerekir.

## Ortam değişkenleri

| Değişken | Zorunlu | Açıklama |
|----------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | evet | `prod` olmalı (`application-prod.yml`) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | evet | PostgreSQL bağlantısı, ör. `jdbc:postgresql://host:5432/lifetrack` |
| `JWT_SECRET` | evet | En az 32 bayt. Üret: `openssl rand -base64 48`. Değişirse herkes yeniden giriş yapar |
| `TZ` | evet | `Europe/Istanbul`. Koşu tarihleri saat dilimsiz saklanır; sunucu kullanıcıyla aynı saat diliminde olmalı |
| `JWT_EXPIRATION_MS` | hayır | Access token süresi, varsayılan `900000` (15 dk) |
| `JWT_REFRESH_EXPIRATION_DAYS` | hayır | Refresh token süresi, varsayılan `30` |
| `TRUSTED_PROXIES` | hayır | `X-Forwarded-For`'una güvenilecek proxy IP'leri (regex). Varsayılan: özel ağ adresleri |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | hayır | Prod'da varsayılan boş. Yalnızca API'yi başka bir web adresinden çağıracaksan doldur |
| `LOGIN_MAX_FAILURES_PER_ACCOUNT`, `LOGIN_MAX_FAILURES_PER_IP`, `LOGIN_FAILURE_WINDOW_MINUTES` | hayır | Hatalı şifre sınırları (5 / 20 / 15 dk) |

Prod profili şunları yapar: Swagger/API dokümanını kapatır, hata yanıtlarında iç ayrıntı göstermez, istemci IP'sini
reverse proxy'nin `X-Forwarded-For` başlığından alır (giriş denemesi sınırı için gerekli) ve
`/actuator/health` sağlık kontrolünü açar. Backend **doğrudan internete açılmamalı**; yalnızca proxy'nin arkasında
çalışmalı, yoksa istemciler `X-Forwarded-For` ile IP'lerini taklit edebilir.

## Seçenek A: Kendi sunucun (VPS) — `docker-compose.prod.yml`

Hazır yapı: Caddy (otomatik HTTPS + web uygulaması + `/api` yönlendirmesi), backend, PostgreSQL ve günlük yedek.
Backend ve veritabanı dışarıya port açmaz.

1. Docker kurulu bir Linux sunucu al (1 GB RAM yeterli, 2 GB rahat). 80 ve 443 portlarını aç.
2. Alan adının DNS'inde bir `A` kaydını sunucunun IP'sine yönlendir.
3. Sunucuda:
   ```bash
   git clone https://github.com/onurercen/lifeTrack.git && cd lifeTrack
   cp deploy/.env.prod.example .env.prod
   # .env.prod içinde DOMAIN, POSTGRES_PASSWORD ve JWT_SECRET değerlerini doldur
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
   ```
4. `https://<alan-adı>` adresini aç. Caddy sertifikayı ilk istekte alır (birkaç saniye sürebilir).

Güncelleme:
```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```
Flyway migration'ları backend açılırken otomatik çalışır.

Loglar: `docker compose -f docker-compose.prod.yml logs -f backend`

## Seçenek B: Railway / Fly.io gibi platformlar

Bu platformlar HTTPS'i ve proxy'yi kendileri sağlar.

- **Backend**: `backend/Dockerfile` ile deploy et. Ortam değişkenlerini yukarıdaki tablodan gir.
  Sağlık kontrolü yolu: `/actuator/health`. Port: `8080`.
- **Veritabanı**: platformun yönetilen PostgreSQL 16'sını ekle ve bağlantı bilgisini `DB_URL` biçimine çevir
  (`jdbc:postgresql://<host>:<port>/<db>`), kullanıcı adı ve şifreyi ayrı değişkenlere yaz.
- **Web uygulaması**: `mobile/Dockerfile` Caddy ile statik dosyaları sunar, ama `/api` yönlendirmesi için
  `deploy/Caddyfile` ve `DOMAIN` gerekir. Platformda en kolayı web uygulamasını ayrı bir servis yerine, platformun
  yönlendirme özelliğiyle aynı alan adında `/api`'yi backend'e göndermektir. Aynı alan adı mümkün değilse web
  uygulamasını `--dart-define=API_BASE_URL=https://<api-adresi>/api` ile derle ve backend'de
  `CORS_ALLOWED_ORIGIN_PATTERNS=https://<web-adresi>` ayarla.
- **Proxy IP'si**: platformun proxy'si özel ağ adresinden gelmiyorsa `TRUSTED_PROXIES` ayarlanmalı, yoksa giriş
  sınırı tüm kullanıcılar için ortak işler. Platformun dokümanında proxy adres aralığına bak.
- **Yedek**: yönetilen PostgreSQL'in otomatik yedeğini aç (`backup` servisi yalnızca Seçenek A'da var).

## Yedekleme ve geri yükleme (Seçenek A)

`backup` servisi açılışta ve sonra her `BACKUP_INTERVAL_HOURS` saatte bir `pg_dump` alır, `./backups` altına
`lifetrack-YYYY-MM-DD_HHMM.dump` olarak yazar ve `BACKUP_KEEP_DAYS` günden eski dosyaları siler.

Yedekler aynı sunucuda durur; sunucu kaybolursa yedekler de gider. Düzenli olarak başka bir yere kopyala, örneğin
kendi bilgisayarından:
```bash
rsync -av kullanici@sunucu:lifeTrack/backups/ ~/lifetrack-backups/
```

Geri yükleme (mevcut verinin **üzerine yazar**):
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres \
  pg_restore --clean --if-exists -U lifetrack -d lifetrack < backups/lifetrack-2026-10-07_0300.dump
docker compose -f docker-compose.prod.yml --env-file .env.prod start backend
```

## Yerelde deneme

Aynı yapı bilgisayarda `DOMAIN=localhost` ile çalışır (Caddy kendi sertifikasını üretir, tarayıcı uyarı verir):
```bash
cp deploy/.env.prod.example .env.prod   # DOMAIN=localhost, HTTP_PORT=8081, HTTPS_PORT=8443 ekle; şifreleri doldur
docker compose -p lifetrack-prod -f docker-compose.prod.yml --env-file .env.prod up -d --build
open https://localhost:8443
```
`-p` ile ayrı bir proje adı vermek, geliştirme için kullanılan `docker compose` veritabanına dokunmamasını sağlar.

## Yayın öncesi kontrol listesi

- [ ] `JWT_SECRET` ve `POSTGRES_PASSWORD` rastgele ve güçlü, `.env.prod` repoya girmiyor
- [ ] `SPRING_PROFILES_ACTIVE=prod` ve `TZ=Europe/Istanbul`
- [ ] Backend ve PostgreSQL portları internete kapalı (yalnızca 80/443 açık)
- [ ] `https://<alan-adı>` açılıyor; sağlık kontrolü dışarıdan erişilemiyor (`/api/actuator/health` → `401`)
- [ ] İlk yedek `./backups` altında oluştu; bir kez geri yükleme denendi
- [ ] Yedekler düzenli olarak sunucu dışına kopyalanıyor
