# LifeTrack API

Base URL: `http://localhost:8080/api`

`/auth/**` dışındaki tüm endpoint'ler `Authorization: Bearer <token>` başlığı ister.
Token yoksa, geçersizse veya süresi dolmuşsa yanıt `401 Unauthorized` olur.

E-postasını doğrulamamış kullanıcılar yalnızca `/auth/**` ve `/users/**` endpoint'lerini kullanabilir. Diğerleri
(koşu, kitap, medya, özet) şu yanıtı döner:

```json
{ "message": "Devam etmek için e-posta adresini doğrulaman gerekiyor", "code": "EMAIL_NOT_VERIFIED" }
```

Durum kodu `403`'tür. Doğrulama sistemi gelmeden önce açılmış hesaplar doğrulanmış sayılır.

## Auth

| Method | Path             | Açıklama                    | Başarılı |
|--------|------------------|-----------------------------|----------|
| POST   | `/auth/register` | Kayıt olur, token çifti döner | 201    |
| POST   | `/auth/login`    | Giriş yapar, token çifti döner | 200   |
| POST   | `/auth/refresh`  | Refresh token ile yeni token çifti alır | 200 |
| POST   | `/auth/logout`   | Refresh token'ı iptal eder  | 204      |
| POST   | `/auth/forgot-password` | Şifre sıfırlama kodu e-postalar | 204 |
| POST   | `/auth/reset-password`  | Kodla yeni şifre belirler, token çifti döner | 200 |

Yanıt (`register`, `login`, `refresh`, `reset-password`):

```json
{
  "token": "<access JWT>", "refreshToken": "<opak anahtar>",
  "user": { "id": 1, "name": "Ayşe", "email": "ayse@test.com", "emailVerified": false }
}
```

- `token` kısa ömürlüdür (varsayılan 15 dk, `JWT_EXPIRATION_MS`) ve `Authorization` başlığında gönderilir.
- `refreshToken` uzun ömürlüdür (varsayılan 30 gün, `JWT_REFRESH_EXPIRATION_DAYS`). Sunucu yalnızca SHA-256 özetini saklar.
- `refresh` ve `logout` gövdesi: `{ "refreshToken": "..." }`.
- Her `refresh` eski refresh token'ı geçersiz kılar ve yenisini döner (rotation). `refresh` ile zaten kullanılmış
  bir token tekrar gönderilirse çalınmış sayılır: kullanıcının **tüm** oturumları kapatılır ve `401` döner.
  Çıkış veya şifre değişikliğiyle iptal edilmiş bir token ise yalnızca `401` alır, diğer oturumlara dokunulmaz.
- Geçersiz, süresi dolmuş veya iptal edilmiş refresh token `401` döner. `logout` her durumda `204` döner.
- Başarısız girişler sınırlıdır (varsayılan 15 dakikada): aynı IP'den aynı e-postaya 5, aynı IP'den toplamda 20
  hatalı deneme. Sınır aşılınca `429` ve saniye cinsinden `Retry-After` başlığı döner; bu sürede doğru şifre de
  kabul edilmez. Başarılı giriş o e-postanın sayacını sıfırlar. Hesap sınırı IP'ye bağlı olduğu için başka bir
  IP'den giriş etkilenmez (e-postayı bilen biri hesabı kilitleyemez). Ayarlar: `LOGIN_MAX_FAILURES_PER_ACCOUNT`,
  `LOGIN_MAX_FAILURES_PER_IP`, `LOGIN_FAILURE_WINDOW_MINUTES`.
- Sayaçlar bellekte tutulur: yeniden başlatınca sıfırlanır ve birden fazla sunucu arasında paylaşılmaz.
  Uygulama bir reverse proxy arkasındaysa istemci IP'si için proxy'nin `X-Forwarded-For` başlığına güvenilecek
  şekilde ayarlanmalıdır, yoksa tüm istemciler proxy'nin IP'sini paylaşır.
- `register` hesabı oluşturur ve e-postaya 6 haneli doğrulama kodu gönderir. Kod `/users/me/verify-email` ile girilir.

### E-posta kodları

Doğrulama ve şifre sıfırlama, e-postayla gönderilen 6 haneli kodlarla çalışır:

- Kod 15 dakika geçerlidir ve tek kullanımlıktır. Sunucu yalnızca bcrypt özetini saklar.
- 5 hatalı denemeden sonra kod silinir. Bu durumda `errors.code` "Çok fazla hatalı deneme. Lütfen yeni kod isteyin." olur.
- Yeni kod en erken 60 saniye sonra istenebilir. Daha önce istenirse doğrulama kodu için `429` ve `Retry-After` döner.
- Hatalı, süresi dolmuş veya hiç gönderilmemiş kod `400` ve `errors.code` döner.

`forgot-password` gövdesi `{ "email": "..." }` şeklindedir ve her durumda `204` döner. Böylece hangi adreslerin kayıtlı
olduğu öğrenilemez: adres kayıtlıysa kod gönderilir, son kodun üzerinden 60 saniye geçmemişse yenisi gönderilmez.
Aynı IP'den 15 dakikada en fazla 20 istek kabul edilir.

`reset-password` gövdesi `{ "email": "...", "code": "123456", "newPassword": "..." }` şeklindedir. Başarılı olursa
şifre değişir, kullanıcının tüm oturumları kapatılır, e-posta doğrulanmış sayılır (kod adresin sahibine gitti) ve
yeni bir token çifti döner. Aynı IP'den 15 dakikada 20 hatalı kod denemesinden sonra `429` döner.

- İstemci akışı: bir istek `401` alırsa `refresh` çağrılır ve istek yeni token ile bir kez tekrarlanır.
  Aynı anda gelen `401`'ler tek bir `refresh` isteğini paylaşmalıdır.

## Kullanıcı

| Method | Path        | Açıklama                      |
|--------|-------------|-------------------------------|
| GET    | `/users/me` | Oturumdaki kullanıcının profili |
| PUT    | `/users/me` | Adı günceller: `{ "name": "Ayşe Yılmaz" }` |
| PUT    | `/users/me/password` | Şifreyi değiştirir: `{ "currentPassword": "...", "newPassword": "..." }` |
| DELETE | `/users/me` | Hesabı ve tüm kayıtları siler: `{ "password": "..." }`, `204` döner |
| POST   | `/users/me/verify-email` | E-postayı doğrular: `{ "code": "123456" }`, güncel profili döner |
| POST   | `/users/me/verify-email/resend` | Yeni doğrulama kodu gönderir, `204` döner (zaten doğrulanmışsa `400`) |
| GET    | `/users/me/export` | Tüm verileri JSON dosyası olarak indirir |

Dışa aktarma yanıtı `Content-Disposition: attachment; filename="lifetrack-YYYY-MM-DD.json"` başlığıyla gelir:

```json
{
  "exportedAt": "2026-10-09T10:00:00",
  "account": { "name": "Ayşe", "email": "ayse@test.com", "createdAt": "2026-01-01T09:00:00" },
  "runs": [ "... koşu yanıtları" ],
  "books": [ "... kitap yanıtları" ],
  "media": [ "... medya yanıtları" ]
}
```

Listeler sayfalı değildir. Kayıtlar, liste endpoint'lerindeki biçimde ve aynı sırayla gelir.

- Şifre değişince kullanıcının tüm refresh token'ları iptal edilir (diğer cihazlar en geç access token
  süresi dolunca çıkış yapar) ve yanıt olarak çağıran cihaz için yeni bir token çifti döner (`/auth/login` yanıtı ile aynı).
- Mevcut şifre yanlışsa `400` ve `errors.currentPassword` (silmede `errors.password`) döner.
  15 dakikada 5 yanlış şifreden sonra bu iki endpoint `429` döner (çalınmış bir token ile şifre denenmesin diye).
- E-posta değiştirme şimdilik desteklenmiyor.

## Sayfalama

Koşu, kitap ve medya listeleri sayfalıdır. `page` (0'dan başlar, varsayılan `0`) ve `size` (1–100, varsayılan `20`)
sorgu parametreleriyle istenir:

```json
{ "items": [ "..." ], "page": 0, "size": 20, "totalItems": 57, "hasNext": true }
```

`hasNext` `true` olduğu sürece sonraki sayfa istenebilir. Geçersiz `page` veya `size` `400` döner
(`errors.page` / `errors.size`). Sıralama her listede sabittir.

## Özet (dashboard)

| Method | Path         | Açıklama |
|--------|--------------|----------|
| GET    | `/dashboard` | Ana sayfa özeti |

```json
{
  "runs": {
    "totalCount": 3, "totalDistanceKm": 18.0,
    "weekCount": 2, "weekDistanceKm": 8.0, "weekDurationMinutes": 50,
    "lastSevenDays": [{ "date": "2026-09-28", "distanceKm": 0.0 }, "... 7 gün, bugün en sonda"]
  },
  "books": {
    "totalCount": 4, "readingCount": 1, "finishedThisYear": 2,
    "currentlyReading": [{ "id": 7, "title": "Dune", "author": "Frank Herbert", "currentPage": 120, "pageCount": 400 }]
  },
  "media": { "totalCount": 2, "inProgressCount": 1, "completedThisYear": 1 }
}
```

- "Hafta" bugün dahil son 7 gündür (takvim haftası değil).
- `finishedThisYear` / `completedThisYear`: bitiş tarihi bu takvim yılında olan kayıtlar.
- `currentlyReading` en fazla 5 kitap içerir (son başlananlar önce). Tamamı için `readingCount`.

## Koşu

| Method | Path    | Açıklama                    |
|--------|---------|-----------------------------|
| GET    | `/runs?page=&size=` | Kullanıcının koşuları (yeniden eskiye, sayfalı) |
| GET    | `/runs/summary` | Tüm koşuların toplamı: `{ "totalCount": 12, "totalDistanceKm": 61.5, "totalDurationMinutes": 340 }` |
| POST   | `/runs` | Koşu ekler                  |
| PUT    | `/runs/{id}` | Koşuyu günceller (gövde POST ile aynı) |
| DELETE | `/runs/{id}` | Koşuyu siler, `204` döner |

Başka bir kullanıcıya ait koşu id'si için `404` döner.

Gövde:

```json
{ "distanceKm": 5.2, "durationMinutes": 30, "caloriesBurned": 420, "notes": "Sabah koşusu", "runAt": "2026-10-04T07:30:00" }
```

- `caloriesBurned`, `notes` ve `runAt` opsiyoneldir.
- `runAt` koşunun yapıldığı yerel zamandır (saat dilimi yok). Eklemede verilmezse şimdiki zaman,
  güncellemede verilmezse mevcut değer kullanılır. Gelecek bir zaman `400` döner.
- Liste `runAt`'e göre yeniden eskiye sıralanır. Yanıtta ayrıca `createdAt` (kaydın oluşturulma anı) bulunur.

## Kitap

| Method | Path                    | Açıklama                                   |
|--------|-------------------------|--------------------------------------------|
| GET    | `/books?query=<metin>&status=<durum>&page=&size=` | Kullanıcının kitapları (sayfalı); `query` (başlık, yazar, açıklama) ve `status` opsiyonel filtre |
| POST   | `/books`                | Kitap ekler                                |
| PUT    | `/books/{id}`           | Kitabı günceller                           |
| DELETE | `/books/{id}`           | Kitabı siler, `204` döner                  |

Gövde:

```json
{
  "title": "Dune", "author": "Frank Herbert", "description": "Bilim kurgu klasiği",
  "status": "READING", "pageCount": 400, "currentPage": 120, "rating": null,
  "startedOn": "2026-09-20", "finishedOn": null
}
```

- `title` ve `author` dışındaki tüm alanlar opsiyoneldir; `description` en fazla 2000 karakter.
- `status`: `WANT_TO_READ` (okunacak), `READING` (okunuyor), `FINISHED` (bitti). Eklemede verilmezse
  `WANT_TO_READ`, güncellemede verilmezse mevcut değer kullanılır.
- `rating` 1–5, `pageCount` ≥ 1, `currentPage` ≥ 0 ve `pageCount`'u geçemez.
- Tarihler (`startedOn`, `finishedOn`) `YYYY-MM-DD` biçimindedir, gelecekte olamaz, bitiş başlangıçtan önce olamaz.
- Duruma göre sunucu şu kuralları uygular:
  - `WANT_TO_READ`: tarihler ve `currentPage` temizlenir.
  - `READING`: `finishedOn` temizlenir. Kitap bu duruma **yeni geçtiyse** ve `startedOn` boşsa bugün yazılır.
  - `FINISHED`: kitap bu duruma yeni geçtiyse ve `finishedOn` boşsa bugün yazılır. `pageCount` varsa `currentPage = pageCount` olur.
- Liste `createdAt`'e göre yeniden eskiye sıralanır.

## Medya

| Method | Path                    | Açıklama                                   |
|--------|-------------------------|--------------------------------------------|
| GET    | `/media?query=<metin>&status=<durum>&page=&size=` | Kullanıcının medyası (sayfalı); `query` (başlık, tür, açıklama) ve `status` opsiyonel filtre |
| POST   | `/media`                | Medya ekler                                |
| PUT    | `/media/{id}`           | Medyayı günceller                          |
| DELETE | `/media/{id}`           | Medyayı siler, `204` döner                 |

Gövde: `{ "title": "Interstellar", "type": "Film", "url": "https://...", "description": "Uzay yolculuğu" }`
(`url` ve `description` opsiyonel; `url` verilirse geçerli bir bağlantı olmalı)

Ek opsiyonel alanlar: `"status": "COMPLETED", "rating": 4, "finishedOn": "2026-10-01"`

- `status`: `PLANNED` (izlenecek), `IN_PROGRESS` (izleniyor), `COMPLETED` (izlendi). Eklemede verilmezse
  `PLANNED`, güncellemede verilmezse mevcut değer kullanılır.
- `rating` 1–5. `finishedOn` yalnızca `COMPLETED` için saklanır, diğer durumlarda temizlenir.
  Kayıt `COMPLETED` durumuna yeni geçtiyse ve tarih boşsa bugün yazılır. Gelecek bir tarih `400` döner.

Kitap ve medyada da başka kullanıcıya ait id için `404` döner.

## Hata formatı

Tüm hatalar en az bir `message` alanı içerir:

```json
{ "message": "Giriş bilgileri hatalı" }
```

Doğrulama hatalarında (ve yanlış şifre gibi alana bağlı hatalarda) alan bazlı mesajlar `errors` altında gelir:

```json
{
  "message": "Gönderilen bilgiler geçersiz",
  "errors": { "distanceKm": "Mesafe zorunludur" }
}
```

| Kod | Ne zaman                                   |
|-----|--------------------------------------------|
| 400 | Doğrulama hatası, okunamayan istek gövdesi, geçersiz `status`/`page`/`size` veya hatalı e-posta kodu |
| 401 | Token yok/geçersiz, ya da hatalı giriş bilgileri |
| 403 | E-posta doğrulanmamış (`code: EMAIL_NOT_VERIFIED`) |
| 404 | Kaynak bulunamadı                          |
| 409 | E-posta zaten kayıtlı                      |
| 429 | Çok fazla başarısız deneme ya da çok sık kod isteği (`Retry-After` başlığıyla) |
| 500 | Beklenmeyen sunucu hatası                  |
