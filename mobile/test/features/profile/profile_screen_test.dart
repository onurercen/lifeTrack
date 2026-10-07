import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app.dart';
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';

import '../../test_helpers.dart';

/// Pumps the whole app signed in as Ayşe and opens the profile tab.
Future<AppDependencies> _openProfile(WidgetTester tester, Map<String, Handler> routes) async {
  late final AppDependencies deps;
  final api = ApiClient(
    baseUrl: 'http://test/api',
    tokenProvider: () => deps.auth.token,
    client: mockBackend({
      'GET /api/users/me': (_) => jsonResponse(testUserJson),
      'GET /api/dashboard': (_) => http.Response('', 500),
      ...routes,
    }),
  );
  deps = AppDependencies.create(api: api);
  await deps.auth.restore();

  await tester.pumpWidget(LifeTrackApp(dependencies: deps));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Profil'));
  await tester.pumpAndSettle();
  return deps;
}

void main() {
  late Map<String, String> store;

  setUp(() => store = useInMemorySecureStorage({'jwt_token': 'valid', 'refresh_token': 'r1'}));

  testWidgets('adı değiştirir ve profilde gösterir', (tester) async {
    Object? sent;
    await _openProfile(tester, {
      'PUT /api/users/me': (request) {
        sent = jsonDecode(request.body);
        return jsonResponse({...testUserJson, 'name': 'Ayşe Yılmaz'});
      },
    });

    await tester.tap(find.text('Adını değiştir'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Ad soyad'), ' Ayşe Yılmaz ');
    await tester.tap(find.text('Kaydet'));
    await tester.pumpAndSettle();

    expect(sent, {'name': 'Ayşe Yılmaz'});
    expect(find.text('Ayşe Yılmaz'), findsOneWidget);
    expect(find.text('Adın güncellendi'), findsOneWidget);
  });

  testWidgets('şifre değişince yeni tokenları kaydeder', (tester) async {
    Object? sent;
    await _openProfile(tester, {
      'PUT /api/users/me/password': (request) {
        sent = jsonDecode(request.body);
        return jsonResponse({'token': 'new-access', 'refreshToken': 'r2', 'user': testUserJson});
      },
    });

    await tester.tap(find.text('Şifreyi değiştir'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Mevcut şifre'), '123456');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre'), 'yeni-sifre');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre (tekrar)'), 'baska');
    await tester.tap(find.widgetWithText(FilledButton, 'Şifreyi değiştir'));
    await tester.pumpAndSettle();
    expect(find.text('Şifreler eşleşmiyor'), findsOneWidget);
    expect(sent, isNull);

    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre (tekrar)'), 'yeni-sifre');
    await tester.tap(find.widgetWithText(FilledButton, 'Şifreyi değiştir'));
    await tester.pumpAndSettle();

    expect(sent, {'currentPassword': '123456', 'newPassword': 'yeni-sifre'});
    expect(store['jwt_token'], 'new-access');
    expect(store['refresh_token'], 'r2');
    expect(find.text('Şifren değiştirildi'), findsOneWidget);
  });

  testWidgets('yanlış mevcut şifreyi alanın altında gösterir', (tester) async {
    await _openProfile(tester, {
      'PUT /api/users/me/password': (_) => jsonResponse({
            'message': 'Şifre hatalı',
            'errors': {'currentPassword': 'Şifre hatalı'},
          }, 400),
    });

    await tester.tap(find.text('Şifreyi değiştir'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Mevcut şifre'), 'yanlis1');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre'), 'yeni-sifre');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yeni şifre (tekrar)'), 'yeni-sifre');
    await tester.tap(find.widgetWithText(FilledButton, 'Şifreyi değiştir'));
    await tester.pumpAndSettle();

    // Under the field and in the snack bar; the form stays open.
    expect(find.text('Şifre hatalı'), findsNWidgets(2));
    expect(store['refresh_token'], 'r1');
  });

  testWidgets('hesap silinince oturum kapanır ve giriş ekranı açılır', (tester) async {
    Object? sent;
    await _openProfile(tester, {
      'DELETE /api/users/me': (request) {
        sent = jsonDecode(request.body);
        return http.Response('', 204);
      },
    });

    await tester.tap(find.text('Hesabı sil'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Şifre'), '123456');
    await tester.tap(find.text('Hesabımı kalıcı olarak sil'));
    await tester.pumpAndSettle();

    expect(sent, {'password': '123456'});
    expect(store, isEmpty);
    expect(find.text('Tekrar hoş geldin'), findsOneWidget);
  });
}
