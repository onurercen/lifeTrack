import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app.dart';
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';

import '../../test_helpers.dart';

Future<AppDependencies> _pumpApp(WidgetTester tester, Map<String, Handler> routes) async {
  late final AppDependencies deps;
  final api = ApiClient(
    baseUrl: 'http://test/api',
    tokenProvider: () => deps.auth.token,
    client: mockBackend({'GET /api/dashboard': (_) => http.Response('', 500), ...routes}),
  );
  deps = AppDependencies.create(api: api);
  await deps.auth.restore();
  await tester.pumpWidget(LifeTrackApp(dependencies: deps));
  await tester.pumpAndSettle();
  return deps;
}

Map<String, dynamic> _session(bool verified) => {
      'token': 'access',
      'refreshToken': 'refresh',
      'user': {...testUserJson, 'emailVerified': verified},
    };

void main() {
  testWidgets('doğrulanmamış hesapta kod ekranı gösterir, doğru kodla uygulamaya geçer', (tester) async {
    useInMemorySecureStorage({'jwt_token': 'valid'});
    final sentCodes = <String>[];
    var resendCount = 0;
    await _pumpApp(tester, {
      'GET /api/users/me': (_) => jsonResponse({...testUserJson, 'emailVerified': false}),
      'POST /api/users/me/verify-email': (request) {
        final code = (jsonDecode(request.body) as Map)['code'] as String;
        sentCodes.add(code);
        if (code != '123456') {
          return jsonResponse({
            'message': 'Gönderilen bilgiler geçersiz',
            'errors': {'code': 'Kod hatalı veya süresi dolmuş'},
          }, 400);
        }
        return jsonResponse({...testUserJson, 'emailVerified': true});
      },
      'POST /api/users/me/verify-email/resend': (_) {
        resendCount++;
        return http.Response('', 204);
      },
    });

    expect(find.text('Kodu e-postana gönderdik'), findsOneWidget);
    expect(find.textContaining('ayse@test.com'), findsOneWidget);
    expect(find.byType(NavigationBar), findsNothing);

    await tester.tap(find.text('Doğrula'));
    await tester.pumpAndSettle();
    expect(find.text('Kod 6 haneli olmalıdır'), findsOneWidget);
    expect(sentCodes, isEmpty);

    await tester.tap(find.text('Kodu tekrar gönder'));
    await tester.pumpAndSettle();
    expect(resendCount, 1);
    expect(find.text('Yeni kod gönderildi.'), findsOneWidget);

    await tester.enterText(find.byType(TextFormField), '000000');
    await tester.tap(find.text('Doğrula'));
    await tester.pumpAndSettle();
    expect(find.text('Kod hatalı veya süresi dolmuş'), findsOneWidget);

    await tester.enterText(find.byType(TextFormField), '123456');
    await tester.tap(find.text('Doğrula'));
    await tester.pumpAndSettle();

    expect(sentCodes, ['000000', '123456']);
    expect(find.byType(NavigationBar), findsOneWidget);
  });

  testWidgets('kayıt olduktan sonra doğrulama ekranına geçer', (tester) async {
    useInMemorySecureStorage();
    await _pumpApp(tester, {'POST /api/auth/register': (_) => jsonResponse(_session(false), 201)});

    await tester.tap(find.text('Hesabın yok mu? Kayıt ol'));
    await tester.pumpAndSettle();
    final fields = find.byType(TextFormField);
    await tester.enterText(fields.at(0), 'Ayşe');
    await tester.enterText(fields.at(1), 'ayse@test.com');
    await tester.enterText(fields.at(2), '123456');
    await tester.enterText(fields.at(3), '123456');
    await tester.tap(find.widgetWithText(FilledButton, 'Kayıt ol'));
    await tester.pumpAndSettle();

    expect(find.text('Kodu e-postana gönderdik'), findsOneWidget);
  });

  testWidgets('şifremi unuttum: kod ister, yeni şifreyle oturum açar', (tester) async {
    final store = useInMemorySecureStorage();
    Map<String, dynamic>? forgotBody;
    Map<String, dynamic>? resetBody;
    await _pumpApp(tester, {
      'POST /api/auth/forgot-password': (request) {
        forgotBody = jsonDecode(request.body) as Map<String, dynamic>;
        return http.Response('', 204);
      },
      'POST /api/auth/reset-password': (request) {
        resetBody = jsonDecode(request.body) as Map<String, dynamic>;
        return jsonResponse(_session(true));
      },
    });

    await tester.enterText(find.widgetWithText(TextFormField, 'E-posta'), 'ayse@test.com');
    await tester.tap(find.text('Şifremi unuttum'));
    await tester.pumpAndSettle();

    // The address typed on the login screen is carried over.
    await tester.tap(find.text('Kod gönder'));
    await tester.pumpAndSettle();
    expect(forgotBody, {'email': 'ayse@test.com'});
    expect(find.textContaining('6 haneli bir kod gönderdik'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextFormField, 'Kod'), '654321');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre'), 'yeni-sifre');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre (tekrar)'), 'baska');
    await tester.tap(find.text('Şifreyi değiştir'));
    await tester.pumpAndSettle();
    expect(find.text('Şifreler eşleşmiyor'), findsOneWidget);
    expect(resetBody, isNull);

    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre (tekrar)'), 'yeni-sifre');
    await tester.tap(find.text('Şifreyi değiştir'));
    await tester.pumpAndSettle();

    expect(resetBody, {'email': 'ayse@test.com', 'code': '654321', 'newPassword': 'yeni-sifre'});
    expect(store['jwt_token'], 'access');
    expect(find.byType(NavigationBar), findsOneWidget);
  });

  testWidgets('şifre sıfırlamada hatalı kodu alanda gösterir', (tester) async {
    useInMemorySecureStorage();
    await _pumpApp(tester, {
      'POST /api/auth/forgot-password': (_) => http.Response('', 204),
      'POST /api/auth/reset-password': (_) => jsonResponse({
            'message': 'Kod hatalı veya süresi dolmuş',
            'errors': {'code': 'Kod hatalı veya süresi dolmuş'},
          }, 400),
    });

    await tester.tap(find.text('Şifremi unuttum'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField), 'ayse@test.com');
    await tester.tap(find.text('Kod gönder'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Kod'), '111111');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre'), 'yeni-sifre');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre (tekrar)'), 'yeni-sifre');
    await tester.tap(find.text('Şifreyi değiştir'));
    await tester.pumpAndSettle();

    expect(find.text('Kod hatalı veya süresi dolmuş'), findsOneWidget);
    expect(find.byType(NavigationBar), findsNothing);
  });
}
