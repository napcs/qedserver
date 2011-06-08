require 'rspec/core/rake_task'
require 'lib/version'

QEDZIPFILE = "qedserver_#{QED_SERVER_VERSION.gsub(".","-")}.zip"
SERVER = "napcs:~/qedserver.napcs.com"

desc "Bundle the gems this thing needs"
task :bundle do
  sh "jruby -S bundle"
end

desc "Create a zip file for distribution to end users"
task :package => :war do
  FileUtils.cp "END_USER_README.md", "README.txt"
  sh "zip -9 #{QEDZIPFILE} LICENSE HISTORY.txt README.txt server.sh server.bat fresh_server.sh fresh_server.bat webserver.war"
  FileUtils.rm "README.txt"
end

desc 'Default: run specs.'
task :default => :spec

desc "Run specs"
RSpec::Core::RakeTask.new do |t|
  t.pattern = "./spec/**/*_spec.rb" # don't need this, it's default.
  # Put spec opts in a file named .rspec in root
end

desc "create the war file" 
task :war do
  FileUtils.rm_rf "public"
  sh "jruby -S warble compiled gemjar executable war"
end

namespace :release do 
  
  desc "update the web site"
  task :website do
    `scp html/* #{SERVER}`
  end

  desc "release new version"
  task :qed => :package do
    `scp #{QEDZIPFILE} #{SERVER}/#{QEDZIPFILE}`
  end

end