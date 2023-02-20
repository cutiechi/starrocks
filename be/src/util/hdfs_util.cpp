// This file is licensed under the Elastic License 2.0. Copyright 2021-present, StarRocks Inc.

#include "util/hdfs_util.h"

#include <common/status.h>
#include <hdfs/hdfs.h>

#include <string>

#include "gutil/strings/substitute.h"
#include "util/error_util.h"

namespace starrocks {

std::string get_hdfs_err_msg() {
    std::string error_msg = get_str_err_msg();
    std::stringstream ss;
    ss << "error=" << error_msg;
    return ss.str();
}

Status get_namenode_from_path(const std::string& path, std::string* namenode) {
    const std::string local_fs("file:/");
    auto n = path.find("://");

    if (n == std::string::npos) {
        if (path.compare(0, local_fs.length(), local_fs) == 0) {
            // Hadoop Path routines strip out consecutive /'s, so recognize 'file:/blah'.
            *namenode = "file:///";
        } else {
            // Path is not qualified, so use the default FS.
            *namenode = "default";
        }
    } else if (n == 0) {
        return Status::InvalidArgument(strings::Substitute("Path missing scheme: $0", path));
    } else {
        // Path is qualified, i.e. "scheme://authority/path/to/file".  Extract
        // "scheme://authority/".
        n = path.find('/', n + 3);
        if (n == std::string::npos) {
            return Status::InvalidArgument(strings::Substitute("Path missing '/' after authority: $0", path));
        } else {
            // Include the trailing '/' for local filesystem case, i.e. "file:///".
            *namenode = path.substr(0, n + 1);
        }
    }
    return Status::OK();
}

} // namespace starrocks
