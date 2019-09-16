find -path ./.git -prune -o -type f -exec sed -i '' -e 's/scala-template/bulyon/g' {} \;
