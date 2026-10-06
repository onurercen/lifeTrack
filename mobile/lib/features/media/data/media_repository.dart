import '../../../core/network/api_client.dart';
import '../models/media.dart';

class MediaRepository {
  const MediaRepository(this._api);

  final ApiClient _api;

  Future<List<Media>> fetchMedia({String? query, MediaStatus? status}) async {
    final params = {
      if (query != null) 'query': query,
      if (status != null) 'status': status.apiValue,
    };
    final json = await _api.get('media', query: params.isEmpty ? null : params) as List<dynamic>;
    return json.map((e) => Media.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Media> createMedia(MediaInput input) async {
    final json = await _api.post('media', body: input.toJson());
    return Media.fromJson(json as Map<String, dynamic>);
  }

  Future<Media> updateMedia(int id, MediaInput input) async {
    final json = await _api.put('media/$id', body: input.toJson());
    return Media.fromJson(json as Map<String, dynamic>);
  }

  Future<void> deleteMedia(int id) => _api.delete('media/$id');
}
