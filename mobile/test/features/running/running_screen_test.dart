import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';
import 'package:lifetrack_mobile/features/running/presentation/running_screen.dart';

import '../../test_helpers.dart';

Map<String, dynamic> _runJson(int id, double km, {String? notes}) => {
      'id': id,
      'distanceKm': km,
      'durationMinutes': 30,
      'caloriesBurned': 300,
      'notes': notes,
      'userEmail': 'ayse@test.com',
      'runAt': '2026-10-04T07:30:00',
      'createdAt': '2026-10-04T07:30:00',
    };

Future<void> _pump(WidgetTester tester, http.Client client) async {
  final deps = AppDependencies.create(api: ApiClient(baseUrl: 'http://test/api', client: client));
  await tester.pumpWidget(
    AppScope(dependencies: deps, child: const MaterialApp(home: RunningScreen())),
  );
  await tester.pumpAndSettle();
}

void main() {
  setUp(() => useInMemorySecureStorage());

  testWidgets('koşu yoksa boş durum mesajı gösterir', (tester) async {
    await _pump(tester, mockBackend({'GET /api/runs': (_) => pageResponse([])}));

    expect(find.textContaining('Henüz koşu eklemedin'), findsOneWidget);
  });

  testWidgets('koşuları ve özeti listeler', (tester) async {
    await _pump(
      tester,
      mockBackend({
        'GET /api/runs': (_) => pageResponse([_runJson(1, 5.0, notes: 'Sabah'), _runJson(2, 10.0)]),
        // Totals come from the server, not from the loaded page.
        'GET /api/runs/summary': (_) =>
            jsonResponse({'totalCount': 12, 'totalDistanceKm': 15.0, 'totalDurationMinutes': 75}),
      }),
    );

    expect(find.text('5 km'), findsOneWidget);
    expect(find.text('10 km'), findsOneWidget);
    expect(find.text('12'), findsOneWidget); // koşu sayısı
    expect(find.text('15'), findsOneWidget); // toplam km
    expect(find.text('5:00 /km'), findsOneWidget); // ortalama tempo
    expect(find.textContaining('Sabah'), findsOneWidget);
  });

  testWidgets('aşağı kaydırınca sonraki sayfayı yükler', (tester) async {
    final pages = <String?>[];
    await _pump(
      tester,
      mockBackend({
        'GET /api/runs': (request) {
          final page = request.url.queryParameters['page'];
          pages.add(page);
          expect(request.url.queryParameters['size'], '20');
          if (page == '0') {
            return pageResponse([for (var i = 1; i <= 20; i++) _runJson(i, i.toDouble())], hasNext: true);
          }
          return pageResponse([_runJson(21, 21.0)], page: 1);
        },
        'GET /api/runs/summary': (_) =>
            jsonResponse({'totalCount': 21, 'totalDistanceKm': 231.0, 'totalDurationMinutes': 630}),
      }),
    );

    expect(pages, ['0']);
    expect(find.text('21 km'), findsNothing);

    await tester.scrollUntilVisible(find.text('21 km'), 300);
    await tester.pumpAndSettle();

    expect(pages, ['0', '1']);
    expect(find.text('21 km'), findsOneWidget);
  });

    testWidgets('sunucu hatasında tekrar dene butonu gösterir', (tester) async {
    await _pump(tester, mockBackend({'GET /api/runs': (_) => http.Response('', 500)}));

    expect(find.text('Tekrar dene'), findsOneWidget);
  });

  testWidgets('formdan yeni koşu ekler ve listeyi yeniler', (tester) async {
    final runs = <Map<String, dynamic>>[];
    Map<String, dynamic>? sentBody;
    await _pump(
      tester,
      mockBackend({
        'GET /api/runs': (_) => pageResponse(runs),
        'POST /api/runs': (request) {
          sentBody = jsonDecode(request.body) as Map<String, dynamic>;
          runs.add(_runJson(1, 5.5));
          return jsonResponse(runs.first, 201);
        },
      }),
    );

    await tester.tap(find.text('Koşu ekle'));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextFormField, 'Mesafe'), '5,5');
    await tester.enterText(find.widgetWithText(TextFormField, 'Süre'), '30');
    await tester.enterText(find.widgetWithText(TextFormField, 'Kalori (opsiyonel)'), '300');
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(sentBody, containsPair('distanceKm', 5.5));
    expect(sentBody, containsPair('durationMinutes', 30));
    expect(sentBody, containsPair('caloriesBurned', 300));
    expect(sentBody, containsPair('notes', null));
    // Defaults to "now", sent as a LocalDateTime without offset.
    final runAt = DateTime.parse(sentBody!['runAt'] as String);
    expect(DateTime.now().difference(runAt).inMinutes, lessThan(1));
    expect(sentBody!['runAt'], isNot(contains('Z')));
    expect(find.text('5,5 km'), findsOneWidget);
  });

  testWidgets('form geçersiz girişte istek göndermez', (tester) async {
    var posted = false;
    await _pump(
      tester,
      mockBackend({
        'GET /api/runs': (_) => pageResponse([]),
        'POST /api/runs': (_) {
          posted = true;
          return jsonResponse({}, 201);
        },
      }),
    );

    await tester.tap(find.text('Koşu ekle'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(find.text('Geçerli bir mesafe giriniz'), findsOneWidget);
    expect(posted, isFalse);
  });

  testWidgets('kaydırıp onaylayınca koşuyu siler', (tester) async {
    var deleted = false;
    await _pump(
      tester,
      mockBackend({
        'GET /api/runs': (_) => pageResponse([_runJson(7, 5.0)]),
        'DELETE /api/runs/7': (_) {
          deleted = true;
          return http.Response('', 204);
        },
      }),
    );

    await tester.drag(find.text('5 km'), const Offset(-500, 0));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Sil'));
    await tester.pumpAndSettle();

    expect(deleted, isTrue);
    expect(find.text('5 km'), findsNothing);
  });

  testWidgets('kalori boş bırakılabilir ve kalorisiz koşu kcal göstermez', (tester) async {
    final runs = <Map<String, dynamic>>[];
    Map<String, dynamic>? sentBody;
    await _pump(
      tester,
      mockBackend({
        'GET /api/runs': (_) => pageResponse(runs),
        'POST /api/runs': (request) {
          sentBody = jsonDecode(request.body) as Map<String, dynamic>;
          runs.add({..._runJson(1, 3.0), 'caloriesBurned': null});
          return jsonResponse(runs.first, 201);
        },
      }),
    );

    await tester.tap(find.text('Koşu ekle'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Mesafe'), '3');
    await tester.enterText(find.widgetWithText(TextFormField, 'Süre'), '20');
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(sentBody, containsPair('caloriesBurned', null));
    expect(find.text('3 km'), findsOneWidget);
    expect(find.textContaining('kcal'), findsNothing);
  });

  testWidgets('düzenlemede koşu tarihi gösterilir ve korunur', (tester) async {
    Map<String, dynamic>? sentBody;
    await _pump(
      tester,
      mockBackend({
        'GET /api/runs': (_) => pageResponse([{..._runJson(4, 5.0), 'runAt': '2026-09-30T18:15:00'}]),
        'PUT /api/runs/4': (request) {
          sentBody = jsonDecode(request.body) as Map<String, dynamic>;
          return jsonResponse(_runJson(4, 6.0));
        },
      }),
    );

    expect(find.textContaining('30 Eyl 2026, 18:15'), findsOneWidget);
    await tester.tap(find.text('5 km'));
    await tester.pumpAndSettle();
    expect(find.text('30 Eyl 2026, 18:15'), findsOneWidget);

    await tester.tap(find.text('Kaydet'));
    await tester.pumpAndSettle();
    expect(sentBody!['runAt'], '2026-09-30T18:15:00');
  });
}
