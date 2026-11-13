document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('.assign-form').forEach(function (form) {
    var route = form.querySelector('[data-route-role]');
    var coordinator = form.querySelector('[data-coordinator]');
    var wrapper = coordinator ? coordinator.closest('.coordinator-field') : null;
    if (!route || !coordinator || !wrapper) return;

    function updateRoute() {
      var selectedRoute = route.value;
      var needsCoordinator = selectedRoute === 'TEACHER' || selectedRoute === 'HOD';
      wrapper.hidden = !needsCoordinator;
      coordinator.disabled = !needsCoordinator;
      coordinator.required = needsCoordinator;
      coordinator.value = '';
      Array.prototype.forEach.call(coordinator.options, function (option) {
        if (!option.value) { option.hidden = false; option.disabled = false; return; }
        var roleMatches = option.dataset.role === selectedRoute;
        option.hidden = needsCoordinator && !roleMatches;
        option.disabled = needsCoordinator && !roleMatches;
      });
    }
    route.addEventListener('change', updateRoute);
    updateRoute();
  });
});
