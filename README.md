# Ochiq Lokatsiya + Kamera Preview

Android Studio uchun Kotlin starter loyiha.

## Nimalar bor?
- Foydalanuvchi tugmani bosgandan keyin GPS koordinatalarini Firebase Realtime Database'ga yuborish.
- Kamera ruxsatini Android oynasi orqali so‘rash.
- Ilova ichida ochiq ko‘rinadigan old/orqa kamera preview.
- Kamera preview'ni to‘xtatish tugmasi.

**Muhim:** kamera tasviri faqat shu telefonda ko‘rinadi. Bu loyiha kamerani masofadan yashirin boshqarmaydi va boshqa telefonga video uzatmaydi. Masofaviy video kerak bo‘lsa, har ikki tomonda rozilik bilan qabul qilinadigan videoqo‘ng‘iroq funksiyasi alohida ishlab chiqilishi kerak.

## Firebase sozlash
1. Firebase Console'da loyiha yarating va `uz.openlocation.app` Android ilovasini qo‘shing.
2. Authentication -> Sign-in method -> Anonymous'ni yoqing.
3. Realtime Database yarating.
4. `google-services.json` faylini `app/` papkasiga joylang.
5. `MainActivity.kt` ichidagi FirebaseDatabase ulanishini o‘z database URL'ingizga moslang.
6. Database Rules'da autentifikatsiya va foydalanuvchi huquqlarini to‘g‘ri cheklang. Ochiq read/write qoidalarini ishlatmang.

## Build
Android Studio'da loyihani oching, Gradle Sync qiling va Run tugmasini bosing. JDK 17 tavsiya etiladi.
