# streamDM Website and Documentation
This is the source for the streamDM website and documentation. It is statically generated using [jekyll](http://jekyllrb.com).

## Site Generation
First install jekyll (assuming you have ruby installed):

```
gem install jekyll
gem install bundler
bundle install
```

Generate the site, and start a server locally:
```
bundle exec jekyll serve -w
```

The `-w` option tells jekyll to watch for changes to files and regenerate the site automatically when any content changes.

Point your browser to http://localhost:4000

By default, jekyll will generate the site in a `_site` directory.


## Publishing the Website
In order to publish the website, you must have committer access to the streamDM website.


To publish changes, copy the content of the `_site` directory to the streamDM web hosting folder.

## Testing locally

To test the website locally, change `url: http://streamdm.noahlab.com.hk` to `url: http://localhost:4000` in the file _config.yml.
