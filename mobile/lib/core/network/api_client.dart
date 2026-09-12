import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../features/auth/models/auth_response.dart';
import 'api_exception.dart';

class ApiClient {
  ApiClient({http.Client? client}) : _client = client ?? http.Client();

  static const String baseUrl = 'http://localhost:8080/api';
  final http.Client _client;

  Future<AuthResponse> login({required String email, required String password}) {
    return _authenticate(
      endpoint: 'auth/login',
      payload: {'email': email, 'password': password},
    );
  }

  Future<AuthResponse> register({
    required String name,
    required String email,
    required String password,
  }) {
    return _authenticate(
      endpoint: 'auth/register',
      payload: {'name': name, 'email': email, 'password': password},
    );
  }

  Future<AuthResponse> _authenticate({
    required String endpoint,
    required Map<String, String> payload,
  }) async {
    final response = await _client.post(
      Uri.parse('$baseUrl/$endpoint'),
      headers: const {'Content-Type': 'application/json'},
      body: jsonEncode(payload),
    );

    final body = _decode(response.body);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
        body['message'] as String? ?? 'İstek başarısız oldu.',
        statusCode: response.statusCode,
      );
    }

    return AuthResponse.fromJson(body);
  }

  Map<String, dynamic> _decode(String body) {
    if (body.isEmpty) return <String, dynamic>{};
    final decoded = jsonDecode(body);
    if (decoded is Map<String, dynamic>) return decoded;
    throw const ApiException('Sunucudan beklenmeyen yanıt alındı.');
  }

  void dispose() => _client.close();
}
