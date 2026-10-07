import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthStorage {
  AuthStorage({FlutterSecureStorage? storage}) : _storage = storage ?? const FlutterSecureStorage();

  static const _tokenKey = 'jwt_token';
  static const _refreshTokenKey = 'refresh_token';
  final FlutterSecureStorage _storage;

  Future<void> saveTokens({required String token, required String refreshToken}) async {
    await _storage.write(key: _tokenKey, value: token);
    await _storage.write(key: _refreshTokenKey, value: refreshToken);
  }

  Future<String?> readToken() => _storage.read(key: _tokenKey);

  Future<String?> readRefreshToken() => _storage.read(key: _refreshTokenKey);

  Future<void> clear() async {
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _refreshTokenKey);
  }
}
