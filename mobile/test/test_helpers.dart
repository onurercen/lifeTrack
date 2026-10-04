import 'dart:convert';

import 'package:flutter_secure_storage/test/test_flutter_secure_storage_platform.dart';
import 'package:flutter_secure_storage_platform_interface/flutter_secure_storage_platform_interface.dart';
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

const testUserJson = {'id': 1, 'name': 'Ayşe', 'email': 'ayse@test.com'};
