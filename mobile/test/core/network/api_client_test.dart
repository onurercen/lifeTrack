import 'dart:async';

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

  test('429 yanıtında sunucu mesajını, yoksa varsayılan mesajı taşır', () async {
    final api = ApiClient(
      baseUrl: 'http://test/api',
      client: mockBackend({
        'POST /api/auth/login': (_) => jsonResponse({'message': 'Lütfen 15 dakika sonra tekrar deneyin.'}, 429),
        'POST /api/auth/register': (_) => http.Response('', 429),
      }),
    );

    await expectLater(
      api.post('auth/login', body: {}, authenticated: false),
      throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Lütfen 15 dakika sonra tekrar deneyin.')),
    );
    await expectLater(
      api.post('auth/register', body: {}, authenticated: false),
      throwsA(isA<ApiException>()
          .having((e) => e.message, 'message', 'Çok fazla deneme yapıldı, lütfen biraz sonra tekrar deneyin.')),
    );
  });

  group('oturum yenileme', () {
    test('401 sonrası oturumu yeniler ve isteği yeni tokenla bir kez tekrarlar', () async {
      var token = 'expired';
      var refreshCalls = 0;
      var unauthorizedCalls = 0;
      final sentTokens = <String?>[];
      final api = ApiClient(
        baseUrl: 'http://test/api',
        tokenProvider: () => token,
        onUnauthorized: () => unauthorizedCalls++,
        client: mockBackend({
          'GET /api/runs': (request) {
            sentTokens.add(request.headers['Authorization']);
            return request.headers['Authorization'] == 'Bearer fresh' ? jsonResponse([]) : http.Response('', 401);
          },
        }),
      )..refreshSession = () async {
          refreshCalls++;
          token = 'fresh';
          return true;
        };

      expect(await api.get('runs'), isEmpty);
      expect(sentTokens, ['Bearer expired', 'Bearer fresh']);
      expect(refreshCalls, 1);
      expect(unauthorizedCalls, 0);
    });

    test('aynı anda gelen 401ler tek bir yenileme isteği paylaşır', () async {
      var token = 'expired';
      var refreshCalls = 0;
      final refreshDone = Completer<void>();
      final api = ApiClient(
        baseUrl: 'http://test/api',
        tokenProvider: () => token,
        client: mockBackend({
          'GET /api/runs': (request) =>
              request.headers['Authorization'] == 'Bearer fresh' ? jsonResponse([]) : http.Response('', 401),
        }),
      )..refreshSession = () async {
          refreshCalls++;
          await refreshDone.future;
          token = 'fresh';
          return true;
        };

      final requests = Future.wait([api.get('runs'), api.get('runs'), api.get('runs')]);
      await Future<void>.delayed(Duration.zero);
      refreshDone.complete();

      expect(await requests, hasLength(3));
      expect(refreshCalls, 1);
    });

    test('yenileme reddedilirse oturum düşer ve istek tekrarlanmaz', () async {
      var calls = 0;
      var unauthorizedCalls = 0;
      final api = ApiClient(
        baseUrl: 'http://test/api',
        tokenProvider: () => 'expired',
        onUnauthorized: () => unauthorizedCalls++,
        client: mockBackend({
          'GET /api/runs': (_) {
            calls++;
            return http.Response('', 401);
          },
        }),
      )..refreshSession = () async => false;

      await expectLater(api.get('runs'), throwsA(isA<ApiException>().having((e) => e.statusCode, 'status', 401)));
      expect(calls, 1);
      expect(unauthorizedCalls, 1);
    });

    test('yenilemeden sonra da 401 gelirse bir daha denemez', () async {
      var refreshCalls = 0;
      var unauthorizedCalls = 0;
      var token = 'a';
      final api = ApiClient(
        baseUrl: 'http://test/api',
        tokenProvider: () => token,
        onUnauthorized: () => unauthorizedCalls++,
        client: mockBackend({'GET /api/runs': (_) => http.Response('', 401)}),
      )..refreshSession = () async {
          refreshCalls++;
          token = 'b';
          return true;
        };

      await expectLater(api.get('runs'), throwsA(isA<ApiException>()));
      expect(refreshCalls, 1);
      expect(unauthorizedCalls, 1);
    });
  });
}
