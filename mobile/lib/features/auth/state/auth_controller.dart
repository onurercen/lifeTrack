import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/storage/auth_storage.dart';
import '../models/auth_response.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

/// Single source of truth for the user's session.
class AuthController extends ChangeNotifier {
  AuthController({required ApiClient api, required AuthStorage storage})
      : _api = api,
        _storage = storage;

  final ApiClient _api;
  final AuthStorage _storage;

  AuthStatus _status = AuthStatus.unknown;
  String? _token;
  AuthUser? _user;

  AuthStatus get status => _status;
  String? get token => _token;
  AuthUser? get user => _user;

  /// Restores a saved session on app start.
  Future<void> restore() async {
    final token = await _storage.readToken();
    if (token == null) {
      _set(AuthStatus.unauthenticated);
      return;
    }

    _token = token;
    try {
      _user = AuthUser.fromJson(await _api.get('users/me') as Map<String, dynamic>);
      _set(AuthStatus.authenticated);
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await _clear();
      } else {
        // Server unreachable: keep the session, the profile is loaded later.
        _set(AuthStatus.authenticated);
      }
    }
  }

  Future<void> login({required String email, required String password}) async {
    final json = await _api.post(
      'auth/login',
      body: {'email': email, 'password': password},
      authenticated: false,
    );
    await _startSession(AuthResponse.fromJson(json as Map<String, dynamic>));
  }

  Future<void> register({
    required String name,
    required String email,
    required String password,
  }) async {
    final json = await _api.post(
      'auth/register',
      body: {'name': name, 'email': email, 'password': password},
      authenticated: false,
    );
    await _startSession(AuthResponse.fromJson(json as Map<String, dynamic>));
  }

  Future<void> logout() => _clear();

  /// Called by [ApiClient] when an authenticated request returns 401.
  void handleUnauthorized() {
    if (_status == AuthStatus.authenticated) _clear();
  }

  Future<void> _startSession(AuthResponse response) async {
    await _storage.saveToken(response.token);
    _token = response.token;
    _user = response.user;
    _set(AuthStatus.authenticated);
  }

  Future<void> _clear() async {
    _token = null;
    _user = null;
    await _storage.clearToken();
    _set(AuthStatus.unauthenticated);
  }

  void _set(AuthStatus status) {
    _status = status;
    notifyListeners();
  }
}
