import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_exception.dart';

typedef TokenProvider = String? Function();

class ApiClient {
  ApiClient({
    http.Client? client,
    String? baseUrl,
    TokenProvider? tokenProvider,
    void Function()? onUnauthorized,
  })  : _client = client ?? http.Client(),
        _baseUrl = baseUrl ?? AppConfig.apiBaseUrl,
        _tokenProvider = tokenProvider,
        _onUnauthorized = onUnauthorized;

  final http.Client _client;
  final String _baseUrl;
  final TokenProvider? _tokenProvider;
  void Function()? _onUnauthorized;

  set onUnauthorized(void Function()? callback) => _onUnauthorized = callback;

  Future<dynamic> get(String path, {Map<String, String>? query, bool authenticated = true}) {
    return _send('GET', path, query: query, authenticated: authenticated);
  }

  Future<dynamic> post(String path, {Object? body, bool authenticated = true}) {
    return _send('POST', path, body: body, authenticated: authenticated);
  }

  Future<dynamic> put(String path, {Object? body}) => _send('PUT', path, body: body);

  Future<dynamic> delete(String path) => _send('DELETE', path);

  Future<dynamic> _send(
    String method,
    String path, {
    Map<String, String>? query,
    Object? body,
    bool authenticated = true,
  }) async {
    final uri = Uri.parse('$_baseUrl/$path').replace(
      queryParameters: (query == null || query.isEmpty) ? null : query,
    );
    final request = http.Request(method, uri)
      ..headers['Accept'] = 'application/json';

    if (authenticated) {
      final token = _tokenProvider?.call();
      if (token != null) request.headers['Authorization'] = 'Bearer $token';
    }
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }

    final http.Response response;
    try {
      final streamed = await _client.send(request).timeout(AppConfig.requestTimeout);
      response = await http.Response.fromStream(streamed);
    } on TimeoutException {
      throw const ApiException('Sunucu yanıt vermedi.');
    } on http.ClientException {
      throw const ApiException('Sunucuya bağlanılamadı.');
    }

    final decoded = _decode(response.body);
    if (response.statusCode >= 200 && response.statusCode < 300) return decoded;

    // A 401 on an authenticated call means the session is no longer valid.
    // On auth endpoints (login) it only means wrong credentials.
    if (response.statusCode == 401 && authenticated) _onUnauthorized?.call();

    final message = decoded is Map<String, dynamic> ? decoded['message'] as String? : null;
    throw ApiException(
      message ?? _defaultMessage(response.statusCode),
      statusCode: response.statusCode,
      fieldErrors: decoded is Map<String, dynamic> && decoded['errors'] is Map
          ? (decoded['errors'] as Map).map((k, v) => MapEntry(k.toString(), v.toString()))
          : const {},
    );
  }

  dynamic _decode(String body) {
    if (body.isEmpty) return null;
    try {
      return jsonDecode(body);
    } on FormatException {
      throw const ApiException('Sunucudan beklenmeyen yanıt alındı.');
    }
  }

  String _defaultMessage(int statusCode) => switch (statusCode) {
        401 => 'Oturumunuzun süresi doldu, lütfen tekrar giriş yapın.',
        403 => 'Bu işlem için yetkiniz yok.',
        404 => 'Kaynak bulunamadı.',
        >= 500 => 'Sunucu hatası, lütfen daha sonra tekrar deneyin.',
        _ => 'İstek başarısız oldu.',
      };

  void dispose() => _client.close();
}
