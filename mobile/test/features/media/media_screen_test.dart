import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';
import 'package:lifetrack_mobile/features/media/models/media.dart';
import 'package:lifetrack_mobile/features/media/presentation/media_screen.dart';

import '../../test_helpers.dart';

Future<void> _pump(WidgetTester tester, http.Client client) async {
  final deps = AppDependencies.create(api: ApiClient(baseUrl: 'http://test/api', client: client));
  await tester.pumpWidget(AppScope(dependencies: deps, child: const MaterialApp(home: MediaScreen())));
  await tester.pumpAndSettle();
}

void main() {
  setUp(() => useInMemorySecureStorage());

  testWidgets('türü seçip yeni medya ekler', (tester) async {
    final items = <Map<String, dynamic>>[];
    Map<String, dynamic>? sent;
    await _pump(
      tester,
      mockBackend({
        'GET /api/media': (_) => jsonResponse(items),
        'POST /api/media': (request) {
          sent = jsonDecode(request.body) as Map<String, dynamic>;
          items.add({...sent!, 'id': 1, 'userEmail': 'a@b.c', 'createdAt': '2026-10-04T07:30:00'});
          return jsonResponse(items.first, 201);
        },
      }),
    );

    expect(find.textContaining('Henüz medya eklemedin'), findsOneWidget);

    await tester.tap(find.text('Medya ekle'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Başlık'), 'Interstellar');
    await tester.tap(find.text('Tür'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Film').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Bağlantı (opsiyonel)'), 'https://example.com/i');
    await tester.enterText(find.widgetWithText(TextFormField, 'Açıklama (opsiyonel)'), 'Uzay yolculuğu');
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(sent, {
      'title': 'Interstellar',
      'type': 'Film',
      'url': 'https://example.com/i',
      'description': 'Uzay yolculuğu',
      'status': 'PLANNED',
      'rating': null,
      'finishedOn': null,
    });
    expect(find.text('Interstellar'), findsOneWidget);
  });

  testWidgets('geçersiz bağlantı ve eksik türde istek göndermez', (tester) async {
    var posted = false;
    await _pump(
      tester,
      mockBackend({
        'GET /api/media': (_) => jsonResponse([]),
        'POST /api/media': (_) {
          posted = true;
          return jsonResponse({}, 201);
        },
      }),
    );

    await tester.tap(find.text('Medya ekle'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Başlık'), 'X');
    await tester.enterText(find.widgetWithText(TextFormField, 'Bağlantı (opsiyonel)'), 'ftp://nope');
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(find.text('Tür zorunludur'), findsOneWidget);
    expect(find.text('Geçerli bir bağlantı giriniz (https://...)'), findsOneWidget);
    expect(posted, isFalse);
  });

  testWidgets('eski tür değeriyle kayıt düzenlenebilir', (tester) async {
    await _pump(
      tester,
      mockBackend({
        'GET /api/media': (_) => jsonResponse([
              {
                'id': 5,
                'title': 'Eski',
                'type': 'movie',
                'url': 'https://e.com',
                'description': 'Eski kayıt',
                'userEmail': 'a@b.c',
                'createdAt': '2026-10-04T07:30:00',
              }
            ]),
      }),
    );

    await tester.tap(find.text('Eski'));
    await tester.pumpAndSettle();

    expect(find.text('Medyayı düzenle'), findsOneWidget);
    expect(find.text('movie'), findsOneWidget);
  });

  testWidgets('bağlantı ve açıklama olmadan medya eklenebilir', (tester) async {
    final items = <Map<String, dynamic>>[];
    Map<String, dynamic>? sent;
    await _pump(
      tester,
      mockBackend({
        'GET /api/media': (_) => jsonResponse(items),
        'POST /api/media': (request) {
          sent = jsonDecode(request.body) as Map<String, dynamic>;
          items.add({...sent!, 'id': 2, 'userEmail': 'a@b.c', 'createdAt': '2026-10-04T07:30:00'});
          return jsonResponse(items.first, 201);
        },
      }),
    );

    await tester.tap(find.text('Medya ekle'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Başlık'), 'Severance');
    await tester.tap(find.text('Tür'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Dizi').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(sent, {
      'title': 'Severance',
      'type': 'Dizi',
      'url': null,
      'description': null,
      'status': 'PLANNED',
      'rating': null,
      'finishedOn': null,
    });
    expect(find.text('Severance'), findsOneWidget);
  });

  testWidgets('izlendi olarak işaretleyip puan verir', (tester) async {
    final items = <Map<String, dynamic>>[];
    Map<String, dynamic>? sent;
    await _pump(
      tester,
      mockBackend({
        'GET /api/media': (_) => jsonResponse(items),
        'POST /api/media': (request) {
          sent = jsonDecode(request.body) as Map<String, dynamic>;
          items.add({...sent!, 'id': 1, 'userEmail': 'a@b.c', 'createdAt': '2026-10-04T07:30:00'});
          return jsonResponse(items.first, 201);
        },
      }),
    );

    await tester.tap(find.text('Medya ekle'));
    await tester.pumpAndSettle();
    expect(find.byTooltip('3 yıldız'), findsNothing);

    await tester.tap(find.descendant(of: find.byType(SegmentedButton<MediaStatus>), matching: find.text('İzlendi')));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Başlık'), 'Dune');
    await tester.tap(find.text('Tür'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Film').last);
    await tester.pumpAndSettle();
    expect(find.text('Bugün'), findsOneWidget);
    await tapInForm(tester, find.byTooltip('3 yıldız'));
    await tapInForm(tester, find.text('Ekle'));

    expect(sent, containsPair('status', 'COMPLETED'));
    expect(sent, containsPair('rating', 3));
    expect(find.text('Film · İzlendi · ★★★☆☆'), findsOneWidget);
  });

  testWidgets('durum filtresiyle listeler', (tester) async {
    final statuses = <String?>[];
    await _pump(
      tester,
      mockBackend({
        'GET /api/media': (request) {
          statuses.add(request.url.queryParameters['status']);
          return jsonResponse([]);
        },
      }),
    );

    await tester.tap(find.widgetWithText(ChoiceChip, 'İzleniyor'));
    await tester.pumpAndSettle();

    expect(statuses.last, 'IN_PROGRESS');
    expect(find.text('Bu filtrede kayıt yok.'), findsOneWidget);
  });
}
