import '../../../core/network/api_client.dart';
import '../../../core/network/page_result.dart';
import '../models/media.dart';

class MediaRepository {
  const MediaRepository(this._api);

  final ApiClient _api;

  Future<PageResult<Media>> fetchMedia({String? query, MediaStatus? status, int page = 0}) async {
    final params = {
      ...PageResult.query(page),
      if (query != null) 'query': query,
      if (status != null) 'status': status.apiValue,
    };
    final json = await _api.get('media', query: params);
    return PageResult.fromJson(json as Map<String, dynamic>, Media.fromJson);
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
