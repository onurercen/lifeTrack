import '../../../core/network/api_client.dart';
import '../models/dashboard_summary.dart';

class DashboardRepository {
  const DashboardRepository(this._api);

  final ApiClient _api;

  Future<DashboardSummary> fetchSummary() async {
    final json = await _api.get('dashboard');
    return DashboardSummary.fromJson(json as Map<String, dynamic>);
  }
}
