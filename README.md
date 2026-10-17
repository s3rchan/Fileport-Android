# Fileport — Android

**Fileport**, Android telefonunuz ile Windows bilgisayarınız arasında **aynı Wi-Fi / yerel ağ (LAN)** üzerinden doğrudan dosya paylaşımı ve mesajlaşma sağlayan bir uygulamadır.

**Güncel Android sürümü: v2.9.1** · **Windows uygulaması:** [Fileport PC](https://github.com/s3rchan/Fileport-PC)

[**⬇ Android APK'yı indir — GitHub Releases**](https://github.com/s3rchan/Fileport-Android/releases/latest) · [**Windows sürümünü indir**](https://github.com/s3rchan/Fileport-PC/releases/latest)

> Fileport, eski oda kodu / WebRTC sunucu projesinden farklıdır. Güncel uygulamada bağlantıyı **Windows PC başlatır**, Android telefon gelen isteği **onaylar**. Yerel dosya aktarımı için harici bir sunucu veya internet gerekmez.

## Öne çıkan özellikler

- **PC'den başlatılan bağlantı:** Windows uygulamasından cihaz seçilip bağlantı isteği gönderilir.
- **Android onay ekranı:** Telefonda gelen isteği **Tamam** veya **İptal** ile yanıtlayabilirsiniz.
- **Güvenilir cihazlar:** İsteğe bağlı **Bu cihaza otomatik bağlan** izni ile sonraki bağlantıları kolaylaştırabilirsiniz.
- **İlk eşleştirmede altı haneli kod:** Her iki cihazdaki güvenlik kodunu karşılaştırıp onaylayın.
- **Dosya aktarımı ve sohbet:** Dosyalar yerel ağda cihazdan cihaza gönderilir; metin mesajları da paylaşılabilir.
- **Yerel geçmiş:** Bağlantı ve aktarım geçmişi cihazda tutulur; gönderilen dosyalar sohbet geçmişine kaydedilmez.
- **Uygulama içinden GitHub güncellemesi:** Yeni sürümler kontrol edilip indirilebilir; Android kurulum için kullanıcı onayı ister.
- **Çevrimdışı APK güncellemesi:** Telefonda bulunan uyumlu bir APK dosyası seçilebilir.
- **Türkçe ve İngilizce arayüz.**

## Sistem gereksinimleri

- **Android 10 (API 29) veya üzeri**.
- Aynı yerel ağda erişilebilir bir Windows bilgisayar ve Fileport PC uygulaması.
- Telefon ile bilgisayar arasında iletişime izin veren Wi-Fi / LAN bağlantısı.

> Yerel dosya aktarımı internetsiz çalışabilir. **GitHub Releases üzerinden sürüm kontrolü ve APK indirme** internet gerektirir.

## Android kurulumu

1. [Fileport Android Releases](https://github.com/s3rchan/Fileport-Android/releases/latest) sayfasını açın.
2. **Assets** bölümündeki `Fileport-Android-vX.Y.Z.apk` dosyasını indirin.
3. Android'in gerektiğinde gösterdiği **bu kaynaktan uygulama yükleme** iznini inceleyip kurulumu onaylayın.
4. Fileport'u açın. İlk eşleştirmede uygulama bilgisayardan gelecek bağlantı isteğini bekler.
5. Windows bilgisayarınızda Fileport PC'yi açın ve telefonunuzu seçerek bağlantıyı başlatın.
6. Telefonda gelen **Bağlantı isteği** ekranını onaylayın. İlk eşleştirmede altı haneli kodları karşılaştırıp iki cihazda da onay verin.
7. Bağlantı sağlandıktan sonra sohbet ve dosya paylaşımını kullanın.

**Not:** Windows bilgisayarının isteği başlatması beklenir. Android tarafında ayrıca **Tekrar bağlan** penceresi veya manuel IP girişine gerek yoktur.

## GitHub üzerinden güncelleme

Fileport Android v2.9.0 sürümünden itibaren uygulama içinden yeni sürüm sorgulanabilir:

1. Fileport'un sağ üstündeki **⋮** menüsünü açın.
2. **GitHub updates / GitHub güncellemeleri → Check for updates / Güncellemeleri denetle** seçeneğine dokunun.
3. Yeni bir sürüm varsa **Download and install / İndir ve yükle** seçeneğini kullanın.
4. İndirme ve APK doğrulaması sonrasında Android'in kurulum onayını tamamlayın.

Güncelleme mekanizması paket kimliğini, sürüm numarasını ve yüklü uygulamayla uyumlu imzalama sertifikasını kontrol eder. Uygulama güncellemeleri **sessizce kurulmaz**; Android kullanıcının onayını ister.

**APK imzası önemlidir:** `Actions` bölümünden indirilen **debug APK**, farklı imzayla üretilebildiği için resmî Release APK'sının üzerine yüklenemeyebilir. Günlük kullanım ve sonraki sürümlere güncelleme için **Releases'deki imzalı APK'yı** tercih edin. Debug sürümünün kaldırılması uygulamadaki yerel verileri silebilir.

## Sürümler

| Sürüm | Başlıca değişiklik |
| --- | --- |
| **v2.9.1** | Android'deki eski yeniden bağlanma / manuel IP penceresi kaldırıldı; PC'nin başlattığı bağlantı akışı korundu. |
| **v2.9.0** | GitHub Releases üzerinden yeni sürüm kontrolü, APK indirme ve kurulum onayı eklendi. |
| **v2.8.0** | Kaynak dosya adları İngilizceleştirildi ve GitHub Actions ile APK derleme altyapısı hazırlandı. |

[**Tüm Android sürümleri**](https://github.com/s3rchan/Fileport-Android/releases)

## Geliştirme ve derleme

Kaynaklar Java tabanlı Android uygulama katmanı ile uygulama içinde çalışan HTML, CSS ve JavaScript arayüzünü içerir. Proje Gradle ile derlenir.

Geliştirici gereksinimleri:

- JDK 17
- Android SDK Platform 35 ve Build Tools 35
- Gradle 8.10.2 (CI ortamında)

```bash
gradle :app:assembleDebug
```

### GitHub Actions

- [**Build Fileport Android APK**](https://github.com/s3rchan/Fileport-Android/actions) — `main` dalındaki değişikliklerden **test amaçlı debug APK** oluşturur ve **Artifacts** bölümüne yükler.
- **Publish Fileport Android Release** — `android-vX.Y.Z` sürüm etiketiyle veya Actions içindeki **Run workflow** düğmesiyle **imzalı Release APK** üretir, imzasını doğrular ve GitHub Releases'e yükler.

İmzalı Release derlemesi için GitHub Actions repository secrets alanında şu değerler yapılandırılmış olmalıdır:

| Secret adı | Amaç |
| --- | --- |
| `FILEPORT_ANDROID_KEYSTORE_BASE64` | Kalıcı imzalama dosyasının Base64 içeriği |
| `FILEPORT_ANDROID_KEYSTORE_PASSWORD` | Keystore şifresi |
| `FILEPORT_ANDROID_KEY_ALIAS` | Anahtar takma adı |
| `FILEPORT_ANDROID_KEY_PASSWORD` | İmzalama anahtarı şifresi |

**İmzalama dosyanızı ve parolalarınızı kaynak kod deposuna yüklemeyin.** Gelecekte uygulamanın aynı kurulum üzerine güncellenebilmesi için aynı imzalama anahtarının güvenle korunması gerekir.

**Android uygulama kimliği:** `com.yakin.app`. Projede kullanılan Java namespace'i `com.fileport.android` olsa da uygulama kimliği uyumlu güncellemeler için korunmaktadır.

## Güvenlik ve gizlilik

- Fileport, yerel ağdaki cihazlar arasında doğrudan iletişim kurar.
- İlk eşleştirmede gösterilen **altı haneli kodu mutlaka karşılaştırın**.
- Tanımadığınız bilgisayarlardan gelen istekleri kabul etmeyin.
- Aktarımda kullanılan **FPTP/1** deneysel bir şifreli uygulama protokolüdür; **bağımsız bir üçüncü taraf güvenlik denetiminden geçmemiştir**.
- GitHub güncellemelerini yalnızca resmî [Fileport Android Releases](https://github.com/s3rchan/Fileport-Android/releases) üzerinden edinin.

Protokol ayrıntıları için [FPTP_PROTOCOL_v1.md](FPTP_PROTOCOL_v1.md) dosyasına bakın.

## Fileport marka kimliği

Gelecek sürümler için belirlenen resmî tasarım standartları:

| Öğe | Değer |
| --- | --- |
| Ana mavi | `#0637E7` |
| Açık mavi | `#368CFF` |
| Lacivert | `#01224F` |
| Açık arka plan | `#CFE1F4` |
| Beyaz | `#FFFFFF` |
| Yazı tipi | **Open Sauce** |
| Logo | **Mavi zemin üzerinde beyaz çapa ve Fileport yazısı** |

> Marka kimliği gelecek uygulama sürümlerinde uygulanacaktır. Burada belirtilmesi mevcut v2.9.1 APK'sının görsellerinin değiştiği anlamına gelmez.

---

**Fileport — Android ve Windows arasında doğrudan dosya aktarımı ve sohbet.**
