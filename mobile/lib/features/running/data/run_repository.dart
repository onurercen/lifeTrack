import '../../../core/network/api_client.dart';
import '../../../core/network/page_result.dart';
import '../models/run.dart';

class RunRepository {
  const RunRepository(this._api);

  final ApiClient _api;

  Future<PageResult<Run>> fetchRuns({int page = 0}) async {
    final json = await _api.get('runs', query: PageResult.query(page));
    return PageResult.fromJson(json as Map<String, dynamic>, Run.fromJson);
  }

  /// Totals over all runs, not just the loaded pages.
  Future<RunSummary> fetchSummary() async {
    return RunSummary.fromJson(await _api.get('runs/summary') as Map<String, dynamic>);
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
