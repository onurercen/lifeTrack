import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/core/network/api_client.dart';
import 'package:lifetrack_mobile/core/network/api_exception.dart';

import '../../test_helpers.dart';

void main() {
  test('kimlik doğrulamalı isteklere Bearer token ekler', () async {
    String? authHeader;
    final api = ApiClient(
      baseUrl: 'http://test/api',
      tokenProvider: () => 'abc',
      client: mockBackend({
        'GET /api/runs': (request) {
          authHeader = request.headers['Authorization'];
          return jsonResponse([]);
        },
      }),
    );

    await api.get('runs');
    expect(authHeader, 'Bearer abc');
  });

  test('auth endpointlerine token eklemez ve 401de oturumu düşürmez', () async {
    var unauthorizedCalls = 0;
    String? authHeader;
    final api = ApiClient(
      baseUrl: 'http://test/api',
      tokenProvider: () => 'abc',
      onUnauthorized: () => unauthorizedCalls++,
      client: mockBackend({
        'POST /api/auth/login': (request) {
          authHeader = request.headers['Authorization'];
          return jsonResponse({'message': 'Giriş bilgileri hatalı'}, 401);
        },
      }),
    );

    await expectLater(
      api.post('auth/login', body: {}, authenticated: false),
      throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Giriş bilgileri hatalı')),
    );
    expect(authHeader, isNull);
    expect(unauthorizedCalls, 0);
  });

  test('kimlik doğrulamalı istekte 401 gelince onUnauthorized çağrılır', () async {
    var unauthorizedCalls = 0;
    final api = ApiClient(
      baseUrl: 'http://test/api',
      tokenProvider: () => 'expired',
      onUnauthorized: () => unauthorizedCalls++,
      client: mockBackend({'GET /api/users/me': (_) => http.Response('', 401)}),
    );

    await expectLater(api.get('users/me'), throwsA(isA<ApiException>()));
    expect(unauthorizedCalls, 1);
  });

  test('doğrulama hatalarını alan bazında taşır', () async {
    final api = ApiClient(
      baseUrl: 'http://test/api',
      client: mockBackend({
        'POST /api/runs': (_) => jsonResponse({
              'message': 'Gönderilen bilgiler geçersiz',
              'errors': {'distanceKm': 'Mesafe zorunludur'},
            }, 400),
      }),
    );

    await expectLater(
      api.post('runs', body: {}),
      throwsA(isA<ApiException>().having((e) => e.fieldErrors['distanceKm'], 'fieldErrors', 'Mesafe zorunludur')),
    );
  });
}
