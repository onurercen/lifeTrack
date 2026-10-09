import 'dart:convert';

import 'package:flutter/widgets.dart';
import 'package:flutter_secure_storage/test/test_flutter_secure_storage_platform.dart';
import 'package:flutter_secure_storage_platform_interface/flutter_secure_storage_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Replaces secure storage with an in-memory map and returns it.
Map<String, String> useInMemorySecureStorage([Map<String, String>? initial]) {
  final store = initial ?? <String, String>{};
  FlutterSecureStoragePlatform.instance = TestFlutterSecureStoragePlatform(store);
  return store;
}

typedef Handler = http.Response Function(http.Request request);

MockClient mockBackend(Map<String, Handler> routes) {
  return MockClient((request) async {
    final key = '${request.method} ${request.url.path}';
    final handler = routes[key];
    if (handler == null) return http.Response('{"message":"not mocked: $key"}', 404);
    return handler(request);
  });
}

http.Response jsonResponse(Object body, [int status = 200]) => http.Response(
      jsonEncode(body),
      status,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );

/// A list endpoint response: one page holding [items].
http.Response pageResponse(List<Object?> items, {bool hasNext = false, int page = 0}) => jsonResponse({
      'items': items,
      'page': page,
      'size': 20,
      'totalItems': items.length,
      'hasNext': hasNext,
    });

const testUserJson = {'id': 1, 'name': 'Ayşe', 'email': 'ayse@test.com', 'emailVerified': true};

/// Scrolls the open form until [finder] is built and visible, then taps it.
Future<void> tapInForm(WidgetTester tester, Finder finder) async {
  await tester.scrollUntilVisible(
    finder,
    200,
    scrollable: find.descendant(of: find.byType(Form), matching: find.byType(Scrollable)).first,
  );
  await tester.tap(finder);
  await tester.pumpAndSettle();
}
