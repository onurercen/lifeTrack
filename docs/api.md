# LifeTrack API

Base URL: `http://localhost:8080/api`

`/auth/**` dışındaki tüm endpoint'ler `Authorization: Bearer <token>` başlığı ister.
Token yoksa, geçersizse veya süresi dolmuşsa yanıt `401 Unauthorized` olur.

## Auth

| Method | Path             | Açıklama                    | Başarılı |
|--------|------------------|-----------------------------|----------|
| POST   | `/auth/register` | Kayıt olur, token çifti döner | 201    |
| POST   | `/auth/login`    | Giriş yapar, token çifti döner | 200   |
| POST   | `/auth/refresh`  | Refresh token ile yeni token çifti alır | 200 |
| POST   | `/auth/logout`   | Refresh token'ı iptal eder  | 204      |

Yanıt (`register`, `login`, `refresh`):

```json
{ "token": "<access JWT>", "refreshToken": "<opak anahtar>", "user": { "id": 1, "name": "Ayşe", "email": "ayse@test.com" } }
```

- `token` kısa ömürlüdür (varsayılan 15 dk, `JWT_EXPIRATION_MS`) ve `Authorization` başlığında gönderilir.
- `refreshToken` uzun ömürlüdür (varsayılan 30 gün, `JWT_REFRESH_EXPIRATION_DAYS`). Sunucu yalnızca SHA-256 özetini saklar.
- `refresh` ve `logout` gövdesi: `{ "refreshToken": "..." }`.
- Her `refresh` eski refresh token'ı geçersiz kılar ve yenisini döner (rotation). `refresh` ile zaten kullanılmış
  bir token tekrar gönderilirse çalınmış sayılır: kullanıcının **tüm** oturumları kapatılır ve `401` döner.
  Çıkış veya şifre değişikliğiyle iptal edilmiş bir token ise yalnızca `401` alır, diğer oturumlara dokunulmaz.
- Geçersiz, süresi dolmuş veya iptal edilmiş refresh token `401` döner. `logout` her durumda `204` döner.
- İstemci akışı: bir istek `401` alırsa `refresh` çağrılır ve istek yeni token ile bir kez tekrarlanır.
  Aynı anda gelen `401`'ler tek bir `refresh` isteğini paylaşmalıdır.

## Kullanıcı

| Method | Path        | Açıklama                      |
|--------|-------------|-------------------------------|
| GET    | `/users/me` | Oturumdaki kullanıcının profili |
| PUT    | `/users/me` | Adı günceller: `{ "name": "Ayşe Yılmaz" }` |
| PUT    | `/users/me/password` | Şifreyi değiştirir: `{ "currentPassword": "...", "newPassword": "..." }` |
| DELETE | `/users/me` | Hesabı ve tüm kayıtları siler: `{ "password": "..." }`, `204` döner |

- Şifre değişince kullanıcının tüm refresh token'ları iptal edilir (diğer cihazlar en geç access token
  süresi dolunca çıkış yapar) ve yanıt olarak çağıran cihaz için yeni bir token çifti döner (`/auth/login` yanıtı ile aynı).
- Mevcut şifre yanlışsa `400` ve `errors.currentPassword` (silmede `errors.password`) döner.
- E-posta değiştirme şimdilik desteklenmiyor.

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
| GET    | `/runs` | Kullanıcının koşuları (yeniden eskiye) |
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
| GET    | `/books?query=<metin>&status=<durum>` | Kullanıcının kitapları; `query` (başlık, yazar, açıklama) ve `status` opsiyonel filtre |
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
| GET    | `/media?query=<metin>&status=<durum>` | Kullanıcının medyası; `query` (başlık, tür, açıklama) ve `status` opsiyonel filtre |
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
| 400 | Doğrulama hatası, okunamayan istek gövdesi veya geçersiz `status` değeri |
| 401 | Token yok/geçersiz, ya da hatalı giriş bilgileri |
| 404 | Kaynak bulunamadı                          |
| 409 | E-posta zaten kayıtlı                      |
| 500 | Beklenmeyen sunucu hatası                  |
