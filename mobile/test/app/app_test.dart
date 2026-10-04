import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifetrack_mobile/app/app.dart';
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';

import '../test_helpers.dart';

AppDependencies _createDeps(Map<String, Handler> routes) {
  late final AppDependencies deps;
  final api = ApiClient(
    baseUrl: 'http://test/api',
    tokenProvider: () => deps.auth.token,
    client: mockBackend(routes),
  );
  deps = AppDependencies.create(api: api);
  return deps;
}

void main() {
  testWidgets('oturum yokken giriş ekranı görünür', (tester) async {
    useInMemorySecureStorage();
    final deps = _createDeps({});
    await deps.auth.restore();

    await tester.pumpWidget(LifeTrackApp(dependencies: deps));

    expect(find.text('Tekrar hoş geldin'), findsOneWidget);
  });

  testWidgets('kayıtlı oturumla ana sayfa ve alt menü görünür, çıkış yapılabilir', (tester) async {
    useInMemorySecureStorage({'jwt_token': 'valid'});
    final deps = _createDeps({'GET /api/users/me': (_) => jsonResponse(testUserJson)});
    await deps.auth.restore();

    await tester.pumpWidget(LifeTrackApp(dependencies: deps));

    expect(find.text('Hoş geldin, Ayşe!'), findsOneWidget);
    expect(find.byType(NavigationBar), findsOneWidget);

    await tester.tap(find.text('Profil'));
    await tester.pumpAndSettle();
    expect(find.text('ayse@test.com'), findsOneWidget);

    await tester.tap(find.text('Çıkış yap').first);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Çıkış yap'));
    await tester.pumpAndSettle();

    expect(find.text('Tekrar hoş geldin'), findsOneWidget);
  });
}
