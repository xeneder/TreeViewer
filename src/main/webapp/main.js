$(document).ready(function() {
    var $node = $('.root');
    setHandlers($node);
});

nodeRefresher = (function() {
	var time;

	var update = function(json, $myNode) {
		$myNode.children('.expander').text('[-]');
		$.each(json, function(i, item) {
			var expander;
			var createOptions;
			if (json[i].type === "directory") {
				expander = '/"><span class="expander">[+]</span> '
				createOptions = '<span class="add-file-option">[Create File]</span> <span class="add-folder-option">[Create Folder]</span> '
			} else {
				expander = '">';
				createOptions = ''
			};
			var $newFolder = $('<div class="node ' + json[i].type + '" path="' + $myNode.attr('path') + json[i].name +
				expander + '<span class="name">' + json[i].name + '</span>' + ' <span class="options">' +
				'<span class="delete-option">[Delete]</span> ' +
				createOptions +
				'<span class="rename-option">[Rename]</span>' +
				'</span>' + '</div>');
			$myNode.append($newFolder);
			setHandlers($newFolder);
		});
		$myNode.addClass('opened');
		$myNode.removeClass('loading');
	}

	var delayedUpdate = function(json, $myNode) {
		if (Date.now() - time >= 2000) {
			update(json, $myNode);
		} else {
			$myNode.children('.expander').text('').append('<img src="loading.gif" alt="Loading...">');
			setTimeout(function() {update(json, $myNode)}, 2000 - (Date.now() - time));
		}
	}

	var fetchAndUpdate = function(onClick, $myNode) {
		console.log($myNode.attr("path"));
		time = Date.now();
		$myNode.children(':not(.expander):not(.name):not(.options)').remove();
		fetch('./folders/', {
				body: '{"type" : "getFolders",' +
						'"path" : "' + $myNode.attr("path") + '"}',
				headers: {
					'content-type': 'application/json'
				},
				method: "POST"
			})
			.then(function(response) {
				return response.json();
			})
			.then(function(json) {
				if (onClick) {
					if (!$myNode.hasClass('opened')) {
						delayedUpdate(json, $myNode);
					} else {
						$myNode.removeClass('opened');
						$myNode.removeClass('loading');
						$myNode.children('.expander').text('[+]');
					}
				} else {
					delayedUpdate(json, $myNode);
				}
			});
	}

	var onClickRefresh = function($myNode) {
		if (!$myNode.hasClass('loading')) {
			$myNode.addClass('loading');
			fetchAndUpdate(true, $myNode);
		}
	}

	var move = function(what, where) {
		fetch('./folders/', {
				body: '{"type" : "move",' +
						'"what" : "' + what + '",' +
						'"where" : "' + where + '"}',
				headers: {
					'content-type': 'application/json'
				},
				method: "POST"
			});
	}

	var del = function(what) {
		fetch('./folders/', {
				body: '{"type" : "delete",' +
						'"what" : "' + what + '"}',
				headers: {
					'content-type': 'application/json'
				},
				method: "POST"
			});
	}

	var fileAction = function(action, path, name, $myNode) {
		fetch('./folders/', {
				body: '{"type" : "' + action + '",' +
						'"path" : "' + path + '",' +
						'"name" : "' + name + '"}',
				headers: {
					'content-type': 'application/json'
				},
				method: "POST"
			})
			.then(function() {
				fetchAndUpdate(false, $myNode);
			});
	}

	return {
		setTime: function(t) {
			time = t;
		},
		callFetchAndUpdate: function(f, $n) {
			fetchAndUpdate(f, $n);
		},
		callOnClickRefresh: function($n) {
			onClickRefresh($n);
		},
		callMove: function(what, where) {
			move(what, where);
		},
		callDelete: function(what) {
			del(what);
		},
		callAddFile: function(path, name, $n) {
			fileAction('createFile', path, name, $n);
		},
		callAddFolder: function(path, name, $n) {
			fileAction('createFolder', path, name, $n);
		},
		callRename: function(path, name, $n) {
			fileAction('rename', path, name, $n);
		}
	};
})();

function setHandlers($node) {
	$node.draggable({handle: ".name", revert: true, stack: ".node", opacity: 0.7});
	if ($node.hasClass('directory')) {
		$node.droppable({
			accept: ".node",
			greedy: true,
			drop: function(event, ui) {
				if (/*$(this).hasClass('opened') && */$(this).attr('path') != ui.draggable.parent().attr('path')) {
					var $dropped = $(ui.draggable).clone();
					setHandlers($dropped);
					$(event.target).append($dropped);
					$(this).find(".ui-draggable-dragging").css({
						top: 0,
						left: 0,
						opacity: 1
					});
					nodeRefresher.callMove(ui.draggable.attr('path'), $(this).attr('path'));
					nodeRefresher.callFetchAndUpdate(false, $(this));
					ui.draggable.remove();
				}
			}
		});
	}

    $node.children('.expander').click(function() {nodeRefresher.callOnClickRefresh($node);});
    $node.children('.options').each(function () {
		console.log($node.attr('path'));
		$(this).children('.delete-option').click(function() {
			nodeRefresher.callDelete($node.attr('path'));
			nodeRefresher.callFetchAndUpdate(false, $node.parent());
		});
		$(this).children('.add-file-option').click(function() {
			$node.append('<input type="text" class="new-file-name" placeholder="Enter new file name..."><input class="new-file-submit" type="submit" value="Submit">');
			$node.children(".new-file-submit").click(function() {
				nodeRefresher.callAddFile($node.attr('path'), $node.find(".new-file-name").val(), $node);
			});
		});
		$(this).children('.add-folder-option').click(function() {
			$node.append('<input type="text" class="new-folder-name" placeholder="Enter new folder name..."><input class="new-folder-submit" type="submit" value="Submit">');
			$node.children(".new-folder-submit").click(function() {
				nodeRefresher.callAddFolder($node.attr('path'), $node.find(".new-folder-name").val(), $node);
			});
		});
		$(this).children('.rename-option').click(function() {
			$node.append('<input type="text" class="rename-name" placeholder="Enter new name..."><input class="rename-submit" type="submit" value="Submit">');
			$node.children(".rename-submit").click(function() {
				nodeRefresher.callRename($node.attr('path'), $node.find(".rename-name").val(), $node.parent());
			});
		});
	});
}