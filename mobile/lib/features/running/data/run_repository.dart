import '../../../core/network/api_client.dart';
import '../models/run.dart';

class RunRepository {
  const RunRepository(this._api);

  final ApiClient _api;

  Future<List<Run>> fetchRuns() async {
    final json = await _api.get('runs') as List<dynamic>;
    return json.map((e) => Run.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Run> createRun(RunInput input) async {
    final json = await _api.post('runs', body: input.toJson());
    return Run.fromJson(json as Map<String, dynamic>);
  }

  Future<Run> updateRun(int id, RunInput input) async {
    final json = await _api.put('runs/$id', body: input.toJson());
    return Run.fromJson(json as Map<String, dynamic>);
  }

  Future<void> deleteRun(int id) => _api.delete('runs/$id');
}
