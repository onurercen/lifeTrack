import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';
import 'package:lifetrack_mobile/core/network/api_exception.dart';
import 'package:lifetrack_mobile/features/auth/state/auth_controller.dart';

import '../../test_helpers.dart';

void main() {
  late Map<String, String> store;

  AppDependencies create(Map<String, Handler> routes) {
    late final AppDependencies deps;
    final api = ApiClient(
      baseUrl: 'http://test/api',
      tokenProvider: () => deps.auth.token,
      client: mockBackend(routes),
    );
    deps = AppDependencies.create(api: api);
    return deps;
  }

  setUp(() => store = useInMemorySecureStorage());

  test('kayıtlı token yoksa oturum kapalı başlar', () async {
    final deps = create({});
    await deps.auth.restore();
    expect(deps.auth.status, AuthStatus.unauthenticated);
  });

  test('geçerli kayıtlı token ile oturum ve profil geri yüklenir', () async {
    store['jwt_token'] = 'valid';
    final deps = create({'GET /api/users/me': (_) => jsonResponse(testUserJson)});

    await deps.auth.restore();

    expect(deps.auth.status, AuthStatus.authenticated);
    expect(deps.auth.user?.name, 'Ayşe');
  });

  test('süresi dolmuş token silinir ve oturum kapanır', () async {
    store['jwt_token'] = 'expired';
    final deps = create({'GET /api/users/me': (_) => http.Response('', 401)});

    await deps.auth.restore();

    expect(deps.auth.status, AuthStatus.unauthenticated);
    expect(store.containsKey('jwt_token'), isFalse);
  });

  test('login tokenları kaydeder, logout siler ve sunucuya bildirir', () async {
    String? loggedOut;
    final deps = create({
      'POST /api/auth/login': (_) =>
          jsonResponse({'token': 'new-token', 'refreshToken': 'new-refresh', 'user': testUserJson}),
      'POST /api/auth/logout': (request) {
        loggedOut = jsonDecode(request.body)['refreshToken'] as String?;
        return http.Response('', 204);
      },
    });

    await deps.auth.login(email: 'ayse@test.com', password: '123456');
    expect(deps.auth.status, AuthStatus.authenticated);
    expect(store['jwt_token'], 'new-token');
    expect(store['refresh_token'], 'new-refresh');

    await deps.auth.logout();
    await pumpEventQueue(); // the server is told in the background
    expect(deps.auth.status, AuthStatus.unauthenticated);
    expect(store, isEmpty);
    expect(loggedOut, 'new-refresh');
  });

  test('erişim tokenı dolmuşsa açılışta refresh token ile oturum yenilenir', () async {
    store
      ..['jwt_token'] = 'expired'
      ..['refresh_token'] = 'refresh-1';
    final deps = create({
      'GET /api/users/me': (request) => request.headers['Authorization'] == 'Bearer fresh'
          ? jsonResponse(testUserJson)
          : http.Response('', 401),
      'POST /api/auth/refresh': (request) {
        expect(jsonDecode(request.body), {'refreshToken': 'refresh-1'});
        return jsonResponse({'token': 'fresh', 'refreshToken': 'refresh-2', 'user': testUserJson});
      },
    });

    await deps.auth.restore();

    expect(deps.auth.status, AuthStatus.authenticated);
    expect(deps.auth.user?.name, 'Ayşe');
    expect(store['jwt_token'], 'fresh');
    expect(store['refresh_token'], 'refresh-2');
  });

  test('refresh token reddedilirse oturum kapanır', () async {
    final deps = create({
      'POST /api/auth/login': (_) =>
          jsonResponse({'token': 'old', 'refreshToken': 'revoked', 'user': testUserJson}),
      'GET /api/runs': (_) => http.Response('', 401),
      'POST /api/auth/refresh': (_) => jsonResponse({'message': 'Oturumunuzun süresi doldu'}, 401),
    });
    await deps.auth.login(email: 'ayse@test.com', password: '123456');

    await expectLater(deps.api.get('runs'), throwsA(isA<ApiException>()));
    await pumpEventQueue(); // handleUnauthorized clears storage asynchronously

    expect(deps.auth.status, AuthStatus.unauthenticated);
    expect(store, isEmpty);
  });

  test('yenileme sırasında ağ hatası olursa oturum korunur', () async {
    final deps = create({
      'POST /api/auth/login': (_) =>
          jsonResponse({'token': 'old', 'refreshToken': 'r1', 'user': testUserJson}),
      'GET /api/runs': (_) => http.Response('', 401),
      'POST /api/auth/refresh': (_) => throw http.ClientException('offline'),
    });
    await deps.auth.login(email: 'ayse@test.com', password: '123456');

    await expectLater(
      deps.api.get('runs'),
      throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Sunucuya bağlanılamadı.')),
    );
    expect(deps.auth.status, AuthStatus.authenticated);
    expect(store['refresh_token'], 'r1');
  });
}
