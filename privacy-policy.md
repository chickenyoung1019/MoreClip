# プライバシーポリシー

**最終更新日: 2026年2月7日**

## 概要

MoreClipボード（以下「本アプリ」）は、ユーザーのプライバシーを尊重し、個人情報の保護に努めています。本プライバシーポリシーでは、本アプリがどのような情報を収集し、どのように使用するかについて説明します。

## 開発者情報

- **アプリ名**: MoreClipボード
- **開発者**: chickenyoung
- **連絡先**: chikenyoung1019@gmail.com

## 収集する情報

### 1. ユーザーが入力した情報

本アプリは、ユーザーがクリップボードに保存したテキストや定型文を、端末内のローカルデータベースに保存します。これらのデータは以下の通りです：
- クリップボードの履歴
- 定型文
- フォルダ情報
- アプリの設定情報

**重要**: これらのデータはすべて端末内に保存され、外部サーバーには送信されません。

### 2. 他アプリからの受信データ

本アプリは、以下の方法で他のアプリからテキストデータを受け取ることができます：
- **テキスト選択メニュー（PROCESS_TEXT）**: 他アプリで選択したテキストを直接保存
- **共有機能**: 他アプリの「共有」メニューから本アプリにテキストを送信

受信したテキストは端末内のローカルデータベースにのみ保存され、外部に送信されることはありません。

### 3. 広告関連の情報（Google AdMob SDKによる自動収集）

本アプリはGoogle AdMobを使用して広告を表示しています。AdMob SDKは以下の情報を自動的に収集します：
- **デバイスまたはその他の識別子**: 広告ID（AAID）
- **おおよその位置情報**: IPアドレスから推定される地域情報
- **アプリのアクティビティ**: アプリの操作情報、広告の表示・クリックに関する情報
- **診断情報**: クラッシュログ、パフォーマンスデータ

これらの情報は広告の配信・パーソナライズ・効果測定の目的で、Googleのサーバーに送信されます。開発者がこれらの情報に直接アクセスすることはありません。

## IME（入力メソッド）機能について

本アプリは、保存した履歴や定型文をどのアプリからでも入力できる専用キーボード（IME）を提供しています。

- **キーストロークの収集**: 本IMEはキーストローク（キー入力）の記録・収集を一切行いません
- **入力内容の送信**: 入力されたテキストを外部サーバーに送信することはありません
- **機能の範囲**: アプリ内に事前保存されたテキスト（履歴・定型文）を選択・入力する機能のみを提供します
- **データの保存**: IME経由で新たにデータを保存・記録することはありません
- **ネットワーク通信**: IME機能はネットワーク通信を行いません（広告表示はメインアプリ画面のみで行われます）

## 使用するパーミッション

| パーミッション | 使用目的 |
|--------------|---------|
| `INTERNET` | Google AdMob広告の読み込み・表示のため |
| `BIND_INPUT_METHOD` | 専用IME（キーボード）機能の提供のため |
| `BIND_QUICK_SETTINGS_TILE` | クイック設定タイルからのアプリ起動のため |

## 情報の使用目的

### ローカルデータ
- ユーザーが入力したデータは、アプリの機能提供のためにのみ使用されます
- データの同期やバックアップ機能は提供していません
- 開発者はこれらのデータにアクセスできません

### 広告データ
- AdMobが収集する情報は、広告の配信とパーソナライズに使用されます
- 詳細は、[Googleのプライバシーポリシー](https://policies.google.com/privacy)をご確認ください

## サードパーティサービス

本アプリは、以下のサードパーティサービスを使用しています：

### Google AdMob
- **目的**: 広告配信
- **プライバシーポリシー**: https://policies.google.com/privacy
- **オプトアウト**: https://adssettings.google.com/

## データの共有

### 開発者による共有
開発者は、ユーザーの個人情報を第三者と共有、販売、または貸与することはありません。ただし、以下の場合を除きます：
- ユーザーの同意がある場合
- 法的義務がある場合

### サードパーティSDKによる共有
本アプリに組み込まれたGoogle AdMob SDKは、広告配信の目的で、前述の広告関連情報をGoogleに送信します。これはアプリの無料提供を維持するために必要な処理です。ユーザーは端末の設定から広告IDをリセットまたは無効化することで、パーソナライズ広告をオプトアウトできます。

## データの保存と削除

- すべてのユーザーデータは端末内にのみ保存されます
- **保持期間**: ユーザーが削除するまで、またはアプリをアンインストールするまで保持されます
- **削除方法**:
  - アプリ内で個別または一括削除
  - 端末の設定からアプリデータを消去
  - アプリをアンインストール（すべてのデータが削除されます）
- AdMobが収集したデータの削除については、[Googleのデータ削除ポリシー](https://policies.google.com/privacy)をご確認ください

## セキュリティ

- ユーザーのデータは端末のローカルストレージ内にのみ保存されます
- データはAndroidのアプリサンドボックスにより、他のアプリからアクセスできないよう保護されています
- ネットワーク通信は広告配信（HTTPS）に限定されます
- ただし、端末のルート化や物理的なアクセスによるデータ漏洩のリスクを完全に排除することはできません

## お子様のプライバシー

本アプリは、16歳未満の子供を対象としていません。16歳未満の子供から故意に個人情報を収集することはありません。保護者の方が、お子様が個人情報を提供したと判明した場合は、下記の連絡先までご連絡ください。速やかに対応いたします。

## 日本のユーザーへの追加情報

個人情報の保護に関する法律に基づき、以下の事項を明示します。

- **個人情報の取得**: 本アプリが直接取得する個人情報はありません。クリップボードデータは端末内にのみ保存されます
- **利用目的**: 端末内データはアプリ機能の提供目的のみに使用します
- **第三者提供**: Google AdMob SDKによる広告関連データの送信を除き、第三者提供は行いません
- **開示等の請求**: 開発者がユーザーの個人データを保持していないため、開示請求の対象となるデータはありません

## EU/EEA地域のユーザーへの追加情報（GDPR）

EU一般データ保護規則（GDPR）に基づき、以下の情報を提供します。

### 処理の法的根拠
- **ローカルデータ**: ユーザーの同意に基づく処理（アプリの使用開始をもって同意とみなします）
- **広告データ**: 正当な利益（アプリの無料提供維持）および同意（パーソナライズ広告について）

### ユーザーの権利
EU/EEA地域のユーザーは、以下の権利を有します：
- **アクセス権**: 保存されているデータの確認（端末内で直接確認可能）
- **削除権**: アプリのアンインストールまたはデータ削除機能で行使可能
- **広告パーソナライズのオプトアウト**: 端末の広告設定から変更可能
- **苦情申立権**: 所管の監督機関への苦情申立て

### データ管理者
chickenyoung（連絡先: chikenyoung1019@gmail.com）

## プライバシーポリシーの変更

本プライバシーポリシーは、必要に応じて更新される場合があります。変更があった場合は、本ページの更新および最終更新日の改訂により通知します。重要な変更がある場合は、アプリ内の通知でもお知らせする場合があります。定期的に本ポリシーを確認することをお勧めします。

## お問い合わせ

本プライバシーポリシーに関するご質問やご不明な点がございましたら、以下までご連絡ください：

**Email**: chikenyoung1019@gmail.com

---

**Privacy Policy (English)**

# Privacy Policy

**Last updated: February 7, 2026**

## Overview

MoreClip Clipboard ("the App") respects user privacy and is committed to protecting personal information. This Privacy Policy explains what information the App collects and how it is used.

## Developer Information

- **App Name**: MoreClip Clipboard
- **Developer**: chickenyoung
- **Contact**: chikenyoung1019@gmail.com

## Information We Collect

### 1. User-Provided Information

The App stores the following data locally on your device:
- Clipboard history
- Templates
- Folder information
- App settings

**Important**: All data is stored locally on your device and is NOT transmitted to external servers.

### 2. Data Received from Other Apps

The App can receive text data from other apps through:
- **Text selection menu (PROCESS_TEXT)**: Directly save text selected in other apps
- **Share feature**: Send text to this App via the "Share" menu in other apps

Received text is stored only in the local database on the device and is never transmitted externally.

### 3. Advertising Information (Automatically Collected by Google AdMob SDK)

The App uses Google AdMob to display advertisements. The AdMob SDK automatically collects the following information:
- **Device or other identifiers**: Advertising ID (AAID)
- **Approximate location**: Regional information estimated from IP address
- **App activity**: App interaction data, ad display and click information
- **Diagnostics**: Crash logs, performance data

This information is transmitted to Google's servers for the purposes of ad delivery, personalization, and performance measurement. The developer does not have direct access to this information.

## IME (Input Method) Feature

The App provides a dedicated keyboard (IME) that allows users to input saved history and templates from any app.

- **Keystroke collection**: This IME does not record or collect any keystrokes
- **Transmission of input**: Text entered through the IME is not transmitted to external servers
- **Scope of functionality**: The IME only provides the ability to select and input text previously saved within the App (history and templates)
- **Data storage**: The IME does not save or record any new data
- **Network communication**: The IME feature does not perform any network communication (advertisements are displayed only on the main app screen)

## Permissions Used

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | Loading and displaying Google AdMob advertisements |
| `BIND_INPUT_METHOD` | Providing the dedicated IME (keyboard) feature |
| `BIND_QUICK_SETTINGS_TILE` | Launching the app from Quick Settings tile |

## How We Use Information

### Local Data
- User data is used solely to provide app functionality
- We do not provide sync or backup features
- The developer cannot access this data

### Advertising Data
- Information collected by AdMob is used for ad delivery and personalization
- See [Google's Privacy Policy](https://policies.google.com/privacy) for details

## Third-Party Services

The App uses the following third-party services:

### Google AdMob
- **Purpose**: Ad delivery
- **Privacy Policy**: https://policies.google.com/privacy
- **Opt-out**: https://adssettings.google.com/

## Data Sharing

### Sharing by the Developer
The developer does not share, sell, or rent user personal information to third parties, except:
- With user consent
- When legally required

### Sharing by Third-Party SDKs
The Google AdMob SDK embedded in this App transmits the aforementioned advertising-related information to Google for the purpose of ad delivery. This is necessary to maintain the free availability of the App. Users can opt out of personalized advertising by resetting or disabling the Advertising ID in their device settings.

## Data Storage and Deletion

- All user data is stored only on the device
- **Retention period**: Data is retained until the user deletes it or uninstalls the App
- **Deletion methods**:
  - Delete individually or in bulk within the App
  - Clear app data from device settings
  - Uninstall the App (all data will be deleted)
- For deletion of data collected by AdMob, please refer to [Google's data deletion policy](https://policies.google.com/privacy)

## Security

- User data is stored only in the device's local storage
- Data is protected by Android's app sandbox, preventing access from other apps
- Network communication is limited to ad delivery (HTTPS)
- However, the risk of data leakage through device rooting or physical access cannot be completely eliminated

## Children's Privacy

The App is not intended for children under 16. The App does not knowingly collect personal information from children under 16. If a parent or guardian becomes aware that their child has provided personal information, please contact us using the information below. We will take prompt action.

## Additional Information for Users in Japan

In accordance with the Act on the Protection of Personal Information, we disclose the following:

- **Collection of personal information**: The App does not directly collect personal information. Clipboard data is stored only on the device
- **Purpose of use**: Data on the device is used solely for providing app functionality
- **Third-party provision**: No third-party provision is made except for advertising-related data transmission by the Google AdMob SDK
- **Disclosure requests**: As the developer does not hold user personal data, there is no data subject to disclosure requests

## Additional Information for EU/EEA Users (GDPR)

In accordance with the EU General Data Protection Regulation (GDPR), we provide the following information.

### Legal Basis for Processing
- **Local data**: Processing based on user consent (consent is deemed given upon starting use of the App)
- **Advertising data**: Legitimate interest (maintaining free availability of the App) and consent (for personalized advertising)

### User Rights
Users in the EU/EEA have the following rights:
- **Right of access**: View stored data (directly accessible on the device)
- **Right to erasure**: Exercisable by uninstalling the App or using the data deletion feature
- **Opt-out of ad personalization**: Changeable through device advertising settings
- **Right to lodge a complaint**: File a complaint with the relevant supervisory authority

### Data Controller
chickenyoung (Contact: chikenyoung1019@gmail.com)

## Changes to This Privacy Policy

This Privacy Policy may be updated as necessary. Changes will be communicated through updates to this page and revision of the last updated date. For significant changes, we may also notify users through in-app notifications. We recommend reviewing this policy periodically.

## Contact Us

If you have questions about this Privacy Policy, please contact:

**Email**: chikenyoung1019@gmail.com
