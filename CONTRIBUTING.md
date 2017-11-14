## How do I submit a contribution?

1. Create a GitHub issue describing the bug or enhancement at https://github.com/huawei-noah/streamDM/issues, please use the appropriate template. 
2. Once you have your contribution ready, that means that follows the coding convention, with sufficient tests, and is organized into logical commits, you may submit a pull request (PR). Prepend the GitHub issue ID to the title of the pull request (e.g, StreamDM-XX: Fancy new feature).
3. **Make sure not to include any merge commits in your PR and always rebase on the current master to make it easier for project committers to apply your changes**
4. Prepend each commit log in a branch with the issue ID. Assuming that you are developing your feature on a branch named after the issue ID (StreamDM-XX):
`git filter-branch -f --msg-filter 'printf "StreamDM-XX: " && cat' StreamDM-XX master..HEAD`
5. The PR will be reviewed and voted upon.

## General guidelines for contributions

1. Contributions should be self-contained, do not create a general issue and append several changes to it that are unrelated. For example, an issue like "update streamDM" that requires changes to the whole framework accompanied by a PR with commits that affect how data sources are used, the instances' representation, add new synthetic data generators, etc, is quite difficult to test as it simply touches every part of the framework. If you want to contribute several changes, create separate issues and separate pull requests. 
2. Follow the best practices for Scala programming. 
3. Follow the framework coding style. For example, learners are parametrized using Option classes, if you try to submit a new learner that receives its configuration through a configuration file, it will require changes to make it similar to how other learners are used. 

## Issues
Issues can be created to describe enhancements or bug reports. Two templates will be available whenever you attempt to create a new Issue. 

1. Before creating an issue make sure to search for it in the [closed issues](https://github.com/huawei-noah/streamDM/issues?utf8=✓&q=is%3Aissue%20is%3Aclosed). If a similar one already exists, but the answer does not address your bug/enhancement, then you shall still create yours, but please refer to the previous issue in it.  
2. Is this a functionality request? E.g. please add support to ...? If so, then use the *Enhancement* template, otherwise use the *Bug report* template. 
3. Fillout the appropriate template below depending on whether you are proposing an *enhancement* (e.g., please add support to ...) or you are creating a *bug report* (e.g., cannot execute learner...). Delete the template that was not used. 
4. Add a descriptive title for the issue. 
