install:
	mvn clean install

site:
	mvn site

	git fetch origin +gh-pages:gh-pages
	rm -rf target/site-repo
	git clone . target/site-repo -b gh-pages --single-branch
	cp -a target/site/. target/site-repo/
	cd target/site-repo && git add -A && git commit -m 'updated site' && git push

site-deploy:
	git push origin gh-pages
