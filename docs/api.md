# LifeTrack API

Base URL: `http://localhost:8080/api`

`/auth/**` dışındaki tüm endpoint'ler `Authorization: Bearer <token>` başlığı ister.
Token yoksa, geçersizse veya süresi dolmuşsa yanıt `401 Unauthorized` olur.

## Auth

| Method | Path             | Açıklama                    | Başarılı |
|--------|------------------|-----------------------------|----------|
| POST   | `/auth/register` | Kayıt olur, token döner     | 201      |
| POST   | `/auth/login`    | Giriş yapar, token döner    | 200      |

## Kullanıcı

| Method | Path        | Açıklama                      |
|--------|-------------|-------------------------------|
| GET    | `/users/me` | Oturumdaki kullanıcının profili |

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
  "bookCount": 4,
  "mediaCount": 2
}
```

"Hafta" bugün dahil son 7 gündür (takvim haftası değil).

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
| GET    | `/books?query=<metin>`  | Kullanıcının kitapları; `query` opsiyonel filtre (başlık, yazar, açıklama) |
| POST   | `/books`                | Kitap ekler                                |
| PUT    | `/books/{id}`           | Kitabı günceller                           |
| DELETE | `/books/{id}`           | Kitabı siler, `204` döner                  |

Gövde: `{ "title": "Dune", "author": "Frank Herbert", "description": "Bilim kurgu klasiği" }`
(`description` opsiyonel, en fazla 2000 karakter)

## Medya

| Method | Path                    | Açıklama                                   |
|--------|-------------------------|--------------------------------------------|
| GET    | `/media?query=<metin>`  | Kullanıcının medyası; `query` opsiyonel filtre (başlık, tür, açıklama) |
| POST   | `/media`                | Medya ekler                                |
| PUT    | `/media/{id}`           | Medyayı günceller                          |
| DELETE | `/media/{id}`           | Medyayı siler, `204` döner                 |

Gövde: `{ "title": "Interstellar", "type": "Film", "url": "https://...", "description": "Uzay yolculuğu" }`
(`url` ve `description` opsiyonel; `url` verilirse geçerli bir bağlantı olmalı)

Kitap ve medyada da başka kullanıcıya ait id için `404` döner.

## Hata formatı

Tüm hatalar en az bir `message` alanı içerir:

```json
{ "message": "Giriş bilgileri hatalı" }
```

Doğrulama hatalarında alan bazlı mesajlar `errors` altında gelir:

```json
{
  "message": "Gönderilen bilgiler geçersiz",
  "errors": { "distanceKm": "Mesafe zorunludur" }
}
```

| Kod | Ne zaman                                   |
|-----|--------------------------------------------|
| 400 | Doğrulama hatası veya okunamayan istek gövdesi |
| 401 | Token yok/geçersiz, ya da hatalı giriş bilgileri |
| 404 | Kaynak bulunamadı                          |
| 409 | E-posta zaten kayıtlı                      |
| 500 | Beklenmeyen sunucu hatası                  |
