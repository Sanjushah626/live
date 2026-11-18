document.addEventListener('DOMContentLoaded', function () {
  function syncAdminThemeButton() {
    var btn = document.getElementById('themeToggleBtn');
    if (!btn) return;
    var isDark = document.documentElement.dataset.theme === 'dark';
    btn.innerHTML = isDark
      ? '<i class="fa-solid fa-sun"></i><span>Bright</span>'
      : '<i class="fa-solid fa-moon"></i><span>Dark</span>';
    btn.setAttribute('aria-pressed', String(isDark));
  }
  syncAdminThemeButton();
  var adminThemeBtn = document.getElementById('themeToggleBtn');
  if (adminThemeBtn) {
    adminThemeBtn.addEventListener('click', function () {
      var next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
      document.documentElement.dataset.theme = next;
      try { localStorage.setItem('myshareapp-theme', next); } catch (e) {}
      syncAdminThemeButton();
    });
  }

  var sidebar = document.getElementById('sidebar');
  var mainContent = document.getElementById('mainContent');
  var sidebarToggleBtn = document.getElementById('sidebarToggleBtn');
  var navbarMenuBtn = document.getElementById('navbarMenuBtn');
  var sidebarBackdrop = document.getElementById('sidebarBackdrop');

  if (sidebarToggleBtn && sidebar && mainContent) {
    sidebarToggleBtn.addEventListener('click', function () {
      sidebar.classList.toggle('collapsed');
      mainContent.classList.toggle('expanded');
    });
  }

  function openMobileSidebar() {
    if (!sidebar || !sidebarBackdrop) return;
    sidebar.classList.add('mobile-open');
    sidebarBackdrop.classList.add('active');
  }

  function closeMobileSidebar() {
    if (!sidebar || !sidebarBackdrop) return;
    sidebar.classList.remove('mobile-open');
    sidebarBackdrop.classList.remove('active');
  }

  if (navbarMenuBtn && sidebar) {
    navbarMenuBtn.addEventListener('click', function () {
      if (sidebar.classList.contains('mobile-open')) {
        closeMobileSidebar();
      } else {
        openMobileSidebar();
      }
    });
  }

  if (sidebarBackdrop) {
    sidebarBackdrop.addEventListener('click', closeMobileSidebar);
  }

  var deleteModal = document.getElementById('deleteModal');
  var deleteModalText = document.getElementById('deleteModalText');
  var deleteModalConfirm = document.getElementById('deleteModalConfirm');
  var deleteModalCancel = document.getElementById('deleteModalCancel');

  document.querySelectorAll('.js-delete-btn').forEach(function (btn) {
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      var url = btn.getAttribute('href') || btn.getAttribute('data-url');
      var message = btn.getAttribute('data-message') || 'This action cannot be undone. Do you really want to delete this item?';
      deleteModalText.textContent = message;
      deleteModalConfirm.setAttribute('href', url);
      deleteModal.classList.add('active');
    });
  });

  if (deleteModalCancel && deleteModal) {
    deleteModalCancel.addEventListener('click', function () {
      deleteModal.classList.remove('active');
    });
  }

  if (deleteModal) {
    deleteModal.addEventListener('click', function (e) {
      if (e.target === deleteModal) {
        deleteModal.classList.remove('active');
      }
    });
  }

  var imageInput = document.getElementById('newsImageInput');
  var previewContainer = document.getElementById('imagePreviewContainer');
  var previewImg = document.getElementById('imagePreviewTag');
  var uploadBox = document.getElementById('imageUploadBox');

  if (imageInput && previewContainer && previewImg) {
    imageInput.addEventListener('change', function (e) {
      var file = e.target.files[0];
      if (!file) {
        previewContainer.style.display = 'none';
        return;
      }

      var allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
      var existingErr = document.getElementById('imageUploadTypeError');
      if (existingErr) existingErr.remove();
      if (allowedTypes.indexOf(file.type) === -1) {
        var errMsg = document.createElement('div');
        errMsg.id = 'imageUploadTypeError';
        errMsg.className = 'alert alert-danger';
        errMsg.style.marginTop = '8px';
        errMsg.textContent = 'Please select a valid image file (JPG, PNG, GIF, or WEBP).';
        var anchor = uploadBox || imageInput;
        anchor.parentNode.insertBefore(errMsg, anchor.nextSibling);
        imageInput.value = '';
        previewContainer.style.display = 'none';
        return;
      }

      var reader = new FileReader();
      reader.onload = function (evt) {
        previewImg.setAttribute('src', evt.target.result);
        previewContainer.style.display = 'block';
      };
      reader.readAsDataURL(file);
    });

    if (uploadBox) {
      uploadBox.addEventListener('click', function () {
        imageInput.click();
      });

      ['dragover', 'dragleave', 'drop'].forEach(function (evtName) {
        uploadBox.addEventListener(evtName, function (e) {
          e.preventDefault();
          e.stopPropagation();
        });
      });

      uploadBox.addEventListener('dragover', function () {
        uploadBox.classList.add('dragover');
      });

      uploadBox.addEventListener('dragleave', function () {
        uploadBox.classList.remove('dragover');
      });

      uploadBox.addEventListener('drop', function (e) {
        uploadBox.classList.remove('dragover');
        if (e.dataTransfer.files.length) {
          imageInput.files = e.dataTransfer.files;
          imageInput.dispatchEvent(new Event('change'));
        }
      });
    }
  }

  document.querySelectorAll('.search-input').forEach(function (input) {
    var targetId = input.getAttribute('data-search-target');
    var table = document.getElementById(targetId);
    if (!table) return;

    input.addEventListener('input', function () {
      var query = input.value.trim().toLowerCase();
      var rows = table.querySelectorAll('tbody tr:not(.no-data-row)');
      var visible = 0;
      rows.forEach(function (row) {
        var text = row.textContent.toLowerCase();
        var match = text.indexOf(query) !== -1;
        row.style.display = match ? '' : 'none';
        if (match) visible++;
      });
      var emptyRow = table.querySelector('tbody tr.search-empty-row');
      if (emptyRow) emptyRow.style.display = visible === 0 && rows.length > 0 ? '' : 'none';
    });
  });

  document.querySelectorAll('.alert.auto-dismiss').forEach(function (alertBox) {
    setTimeout(function () {
      alertBox.style.transition = 'opacity 0.4s ease';
      alertBox.style.opacity = '0';
      setTimeout(function () {
        alertBox.style.display = 'none';
      }, 400);
    }, 4000);
  });
});
