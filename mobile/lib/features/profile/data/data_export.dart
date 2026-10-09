import 'dart:convert';
import 'dart:ui';

import 'package:share_plus/share_plus.dart';

import '../../../core/network/api_client.dart';
import '../../../core/utils/file_sharer.dart';

/// Downloads everything the user stored as a JSON file.
class DataExporter {
  const DataExporter(this._api, this._share);

  final ApiClient _api;
  final FileSharer _share;

  Future<void> export({Rect? origin}) async {
    final json = await _api.get('users/me/export');
    final bytes = utf8.encode(const JsonEncoder.withIndent('  ').convert(json));
    final now = DateTime.now();
    final date = '${now.year}-${_two(now.month)}-${_two(now.day)}';
    final name = 'lifetrack-$date.json';
    await _share(XFile.fromData(bytes, name: name, mimeType: 'application/json'), name, origin: origin);
  }

  static String _two(int value) => value.toString().padLeft(2, '0');
}
