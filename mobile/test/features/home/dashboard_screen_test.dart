import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';
import 'package:lifetrack_mobile/features/home/presentation/dashboard_screen.dart';

import '../../test_helpers.dart';

Map<String, dynamic> _summaryJson({
  double weekKm = 8,
  int weekCount = 2,
  List<Map<String, dynamic>> reading = const [],
}) =>
    {
      'runs': {
        'totalCount': 3,
        'totalDistanceKm': 18.0,
        'weekCount': weekCount,
        'weekDistanceKm': weekKm,
        'weekDurationMinutes': 50,
        'lastSevenDays': [
          for (final (i, date) in const [
            '2026-09-28', '2026-09-29', '2026-09-30', '2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04',
          ].indexed)
            {'date': date, 'distanceKm': i == 6 ? 5.0 : (i == 4 ? 3.0 : 0.0)},
        ],
      },
      'books': {'totalCount': 4, 'readingCount': reading.length, 'finishedThisYear': 1, 'currentlyReading': reading},
      'media': {'totalCount': 2, 'inProgressCount': 0, 'completedThisYear': 2},
    };

Future<void> _pump(WidgetTester tester, http.Client client) async {
  final deps = AppDependencies.create(api: ApiClient(baseUrl: 'http://test/api', client: client));
  await tester.pumpWidget(AppScope(dependencies: deps, child: const MaterialApp(home: DashboardScreen())));
  await tester.pumpAndSettle();
}

void main() {
  setUp(() => useInMemorySecureStorage());

  testWidgets('haftalık özeti, grafiği ve sayaçları gösterir', (tester) async {
    await _pump(tester, mockBackend({'GET /api/dashboard': (_) => jsonResponse(_summaryJson())}));

    expect(find.text('8 km'), findsOneWidget);
    expect(find.text('2 koşu · 50 dk'), findsOneWidget);
    expect(find.text('18 km'), findsOneWidget); // toplam
    expect(find.text('4'), findsOneWidget); // kitap
    expect(find.text('2'), findsOneWidget); // medya
    expect(find.text('1 bu yıl bitti'), findsOneWidget);
    expect(find.text('2 bu yıl izlendi'), findsOneWidget);
    expect(find.text('Şu an okuyorum'), findsNothing);
    expect(find.byType(WeeklyDistanceChart), findsOneWidget);
    expect(find.bySemanticsLabel(RegExp(r'5 km$')), findsOneWidget);
  });

  testWidgets('bu hafta koşu yoksa teşvik mesajı gösterir', (tester) async {
    await _pump(
      tester,
      mockBackend({'GET /api/dashboard': (_) => jsonResponse(_summaryJson(weekKm: 0, weekCount: 0))}),
    );

    expect(find.text('Bu hafta henüz koşmadın'), findsOneWidget);
  });

  testWidgets('hata olursa tekrar denenebilir', (tester) async {
    var calls = 0;
    await _pump(
      tester,
      mockBackend({
        'GET /api/dashboard': (_) => ++calls == 1 ? http.Response('', 500) : jsonResponse(_summaryJson()),
      }),
    );

    expect(find.text('Tekrar dene'), findsOneWidget);
    await tester.tap(find.text('Tekrar dene'));
    await tester.pumpAndSettle();
    expect(find.text('8 km'), findsOneWidget);
  });

  testWidgets('okunmakta olan kitapları ilerlemesiyle gösterir', (tester) async {
    await _pump(
      tester,
      mockBackend({
        'GET /api/dashboard': (_) => jsonResponse(_summaryJson(reading: [
              {'id': 1, 'title': 'Dune', 'author': 'Frank Herbert', 'currentPage': 100, 'pageCount': 400},
              {'id': 2, 'title': 'Sayfasız', 'author': 'Yazar', 'currentPage': null, 'pageCount': null},
            ])),
      }),
    );

    await tester.scrollUntilVisible(find.text('Sayfasız'), 100);
    expect(find.text('Şu an okuyorum'), findsOneWidget);
    expect(find.text('Frank Herbert · 100/400 sayfa'), findsOneWidget);
    expect(find.text('Yazar'), findsOneWidget);
    final bar = tester.widget<LinearProgressIndicator>(find.byType(LinearProgressIndicator));
    expect(bar.value, 0.25);
  });
}
