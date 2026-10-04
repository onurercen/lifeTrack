import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';
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

  test('login tokenı kaydeder, logout siler', () async {
    final deps = create({
      'POST /api/auth/login': (_) => jsonResponse({'token': 'new-token', 'user': testUserJson}),
    });

    await deps.auth.login(email: 'ayse@test.com', password: '123456');
    expect(deps.auth.status, AuthStatus.authenticated);
    expect(store['jwt_token'], 'new-token');

    await deps.auth.logout();
    expect(deps.auth.status, AuthStatus.unauthenticated);
    expect(store.containsKey('jwt_token'), isFalse);
  });
}
